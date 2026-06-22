#lang racket/base

;; #33 L2 datum-IR spike — `--build-edn` compiles straight from claim triples,
;; skipping the text round-trip (claims → EDN → datum→src → TEXT →
;; read-beagle-syntax → datum). The text trip is pure overhead because
;; `datum->src` (render) and `read-beagle-syntax` (build) are exact inverses over
;; the SAME datum, and `edn-triples->datum` already reconstructs that datum from
;; triples.
;;
;; This guard pins the SOUNDNESS INVARIANT the cut rests on: the datum the
;; compiler would get from the claim triples is byte-identical to the datum it
;; gets from the reader. Identical datums ⇒ identical parse/check/emit — so
;; `--build-edn` produces the same program as the text path (the slice-1 srcloc
;; degradation aside: edn-triples->datum yields a bare datum, no line/col claims
;; yet). If the reader and the round-trip ever diverge, this goes red.

(require rackunit
         racket/file
         racket/port
         racket/string
         beagle/private/parse
         (only-in beagle/private/claims-roundtrip
                  datum->edn-lines edn-triples->datum
                  stx->edn-lines edn-triples->syntax
                  emit-edn-typed-file))

;; The forms the READER produces for a module.
(define (reader-forms src)
  (define tmp (make-temporary-file "edn33-~a.bclj"))
  (dynamic-wind
   void
   (lambda ()
     (call-with-output-file tmp (lambda (o) (display src o)) #:exists 'truncate/replace)
     (map syntax->datum (read-beagle-syntax tmp)))
   (lambda () (delete-file tmp))))

;; The forms recovered through claims — mirrors --build-edn EXACTLY: serialize to
;; EDN triple lines (datum->edn-lines, as --emit-edn writes), parse each line
;; (as read-edn-triples does), reconstruct (edn-triples->datum), drop the
;; (beagle-file …) wrapper head.
(define (datum-ir-forms forms)
  (define lines (datum->edn-lines (cons 'beagle-file forms)))
  (define triples (map (lambda (l) (read (open-input-string l))) lines))
  (define wrapped (edn-triples->datum triples))
  (cdr wrapped))

(define SRC
  (string-append
   "#lang beagle/clj\n"
   "(def ^:dynamic *ctx* \"root\")\n"
   "(defn area [w :- Int h :- Int] :- Int (* w h))\n"
   "(defn label [xs :- (Vec String)] :- String\n"
   "  (let [n (count xs)]\n"
   "    (str \"items:\" n)))\n"
   "(def cfg {:enable true :tags #{:a :b}})\n"
   "(def nested [[1 2] {:k [3 4]} #{:x}])\n"
   "(defn pipe [x :- Int] :- Int (-> x (+ 1) (* 2)))\n"))

(test-case "datum-IR round-trip is identity — the --build-edn soundness invariant"
  (define reader (reader-forms SRC))
  (define via-claims (datum-ir-forms reader))
  (check-equal? via-claims reader
                "the datum recovered from claim triples must equal the reader's datum"))

(test-case "round-trip is faithful form-by-form (localizes any drift)"
  (define reader (reader-forms SRC))
  (define via-claims (datum-ir-forms reader))
  (check-equal? (length via-claims) (length reader))
  (for ([r (in-list reader)] [c (in-list via-claims)])
    (check-equal? c r (format "form drifted through claims: ~s" r))))

;; --- slice-2: srcloc claims carry through (closes the slice-1 srcloc gap) -----
;; stx->claims emits per-node line/col/pos/span; edn-triples->syntax rebuilds
;; SYNTAX with those positions. So --build-edn emits byte-identical ^{:line :file}
;; provenance + blame to the text path (verified end-to-end at the CLI; here we
;; pin the unit invariant: pos survives the claim round-trip, datum unchanged).
(define (datum-ir-syntax-forms src-path)
  (define stxs (read-beagle-syntax src-path))
  (define lines (stx->edn-lines (datum->syntax #f (cons 'beagle-file stxs))))
  (define triples (map (lambda (l) (read (open-input-string l))) lines))
  (define wrapper (edn-triples->syntax triples (string->symbol "test-src")))
  (values stxs (cdr (syntax->list wrapper))))   ; drop (beagle-file …) head

(test-case "slice-2: syntax-positions survive the claim round-trip (and datum unchanged)"
  (define tmp (make-temporary-file "edn33s-~a.bclj"))
  (dynamic-wind
   void
   (lambda ()
     (call-with-output-file tmp (lambda (o) (display SRC o)) #:exists 'truncate/replace)
     (define-values (reader-stxs ir-stxs) (datum-ir-syntax-forms tmp))
     (check-equal? (length ir-stxs) (length reader-stxs))
     (for ([r (in-list reader-stxs)] [c (in-list ir-stxs)])
       ;; datum identity preserved
       (check-equal? (syntax->datum c) (syntax->datum r))
       ;; the load-bearing srcloc field (codepoint pos) carried through
       (check-equal? (syntax-position c) (syntax-position r)
                     (format "pos dropped for: ~s" (syntax->datum r)))
       (check-equal? (syntax-span c) (syntax-span r))))
   (lambda () (delete-file tmp))))

;; --- slice-3: the TYPED layer — derived [node "type" T] claims, additive -------
;; --emit-edn-typed emits the slice-2 datum+srcloc claims PLUS each checked node's
;; inferred type, joined to the datum node by (pos,span). Types are DERIVED +
;; ADDITIVE: the build path ignores them, so the datum still reconstructs.
(define (typed-triples src)
  (define tmp (make-temporary-file "edn33t-~a.bclj"))
  (call-with-output-file tmp (lambda (o) (display src o)) #:exists 'truncate/replace)
  (define out (with-output-to-string (lambda () (emit-edn-typed-file tmp))))
  (delete-file tmp)
  (for/list ([line (in-list (string-split out "\n"))]
             #:when (and (> (string-length line) 0) (char=? (string-ref line 0) #\[)))
    (read (open-input-string line))))

(test-case "slice-3: typed dump carries [node \"type\" T] claims joined by (pos,span)"
  (define triples (typed-triples SRC))
  (define type-claims (filter (lambda (t) (equal? (cadr t) "type")) triples))
  (check-true (>= (length type-claims) 1) "expected at least one inferred type claim")
  ;; every type claim attaches to a node that ALSO has kind + pos + span (a real
  ;; datum node, i.e. the (pos,span) join landed on a structural node)
  (define by-id (make-hash))
  (for ([t (in-list triples)])
    (hash-update! by-id (car t) (lambda (h) (hash-set! h (cadr t) (caddr t)) h) (lambda () (make-hash))))
  (for ([tc (in-list type-claims)])
    (define h (hash-ref by-id (car tc)))
    (check-true (and (hash-has-key? h "kind") (hash-has-key? h "pos") (hash-has-key? h "span"))
                (format "type claim on node ~a lacks kind/pos/span" (car tc)))))

(test-case "slice-3: type claims are ADDITIVE — datum still reconstructs (build ignores types)"
  (define triples (typed-triples SRC))
  (define wrapped (edn-triples->datum triples))   ; consumes the FULL typed dump
  (check-equal? (cdr wrapped) (reader-forms SRC)
                "type claims must not perturb the reconstructed datum"))
