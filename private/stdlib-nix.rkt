#lang racket/base

;; Nix-specific stdlib type declarations.
;; Maps Nix builtins and lib.* functions to Beagle types.

(require "types.rkt")

(provide STDLIB-NIX)

(define (fn-of params ret)
  (type-fn (map (lambda (p) (type-prim p)) params)
           #f
           (type-prim ret)))

(define STDLIB-NIX
  (hash
   ;; --- builtins ---
   'builtins/length     (fn-of '(Any) 'Int)
   'builtins/head       (fn-of '(Any) 'Any)
   'builtins/tail       (fn-of '(Any) 'Any)
   'builtins/map        (fn-of '(Any Any) 'Any)
   'builtins/filter     (fn-of '(Any Any) 'Any)
   'builtins/foldl      (fn-of '(Any Any Any) 'Any)
   'builtins/sort       (fn-of '(Any Any) 'Any)
   'builtins/concatLists (fn-of '(Any) 'Any)
   'builtins/concatMap  (fn-of '(Any Any) 'Any)
   'builtins/genList    (fn-of '(Any Int) 'Any)
   'builtins/elem       (fn-of '(Any Any) 'Bool)
   'builtins/all        (fn-of '(Any Any) 'Bool)
   'builtins/any        (fn-of '(Any Any) 'Bool)

   'builtins/attrNames  (fn-of '(Any) 'Any)
   'builtins/attrValues (fn-of '(Any) 'Any)
   'builtins/hasAttr    (fn-of '(String Any) 'Bool)
   'builtins/getAttr    (fn-of '(String Any) 'Any)
   'builtins/removeAttrs (fn-of '(Any Any) 'Any)
   'builtins/intersectAttrs (fn-of '(Any Any) 'Any)
   'builtins/mapAttrs   (fn-of '(Any Any) 'Any)
   'builtins/catAttrs   (fn-of '(String Any) 'Any)
   'builtins/listToAttrs (fn-of '(Any) 'Any)

   'builtins/isString   (fn-of '(Any) 'Bool)
   'builtins/isInt      (fn-of '(Any) 'Bool)
   'builtins/isBool     (fn-of '(Any) 'Bool)
   'builtins/isFloat    (fn-of '(Any) 'Bool)
   'builtins/isList     (fn-of '(Any) 'Bool)
   'builtins/isAttrs    (fn-of '(Any) 'Bool)
   'builtins/isNull     (fn-of '(Any) 'Bool)
   'builtins/isFunction (fn-of '(Any) 'Bool)
   'builtins/isPath     (fn-of '(Any) 'Bool)
   'builtins/typeOf     (fn-of '(Any) 'String)

   'builtins/toString   (fn-of '(Any) 'String)
   'builtins/toJSON     (fn-of '(Any) 'String)
   'builtins/fromJSON   (fn-of '(String) 'Any)
   'builtins/toFile     (fn-of '(String String) 'Any)
   'builtins/readFile   (fn-of '(Any) 'String)
   'builtins/pathExists (fn-of '(Any) 'Bool)

   'builtins/replaceStrings (fn-of '(Any Any String) 'String)
   'builtins/substring  (fn-of '(Int Int String) 'String)
   'builtins/stringLength (fn-of '(String) 'Int)
   'builtins/split      (fn-of '(String String) 'Any)
   'builtins/match      (fn-of '(String String) 'Any)

   'builtins/import     (fn-of '(Any) 'Any)
   'builtins/fetchurl   (fn-of '(String) 'Any)
   'builtins/fetchTarball (fn-of '(Any) 'Any)
   'builtins/trace      (fn-of '(Any Any) 'Any)
   'builtins/tryEval    (fn-of '(Any) 'Any)
   'builtins/throw      (fn-of '(String) 'Any)
   'builtins/abort      (fn-of '(String) 'Any)
   'builtins/deepSeq    (fn-of '(Any Any) 'Any)
   'builtins/seq        (fn-of '(Any Any) 'Any)

   'builtins/currentSystem (type-prim 'String)
   'builtins/storeDir   (type-prim 'String)
   'builtins/nixVersion (type-prim 'String)

   ;; --- lib.* (NixOS module system) ---
   'lib/mkIf            (fn-of '(Bool Any) 'Any)
   'lib/mkMerge         (fn-of '(Any) 'Any)
   'lib/mkDefault       (fn-of '(Any) 'Any)
   'lib/mkForce         (fn-of '(Any) 'Any)
   'lib/mkOverride      (fn-of '(Int Any) 'Any)
   'lib/mkEnableOption  (fn-of '(String) 'Any)
   'lib/mkOption        (fn-of '(Any) 'Any)
   'lib/mkPackageOption (fn-of '(Any) 'Any)

   'lib/optional        (fn-of '(Bool Any) 'Any)
   'lib/optionals       (fn-of '(Bool Any) 'Any)
   'lib/optionalString  (fn-of '(Bool String) 'String)
   'lib/optionalAttrs   (fn-of '(Bool Any) 'Any)

   'lib/concatStrings   (fn-of '(Any) 'String)
   'lib/concatStringsSep (fn-of '(String Any) 'String)
   'lib/concatMapStrings (fn-of '(Any Any) 'String)
   'lib/concatMapStringsSep (fn-of '(String Any Any) 'String)

   'lib/filterAttrs     (fn-of '(Any Any) 'Any)
   'lib/mapAttrs        (fn-of '(Any Any) 'Any)
   'lib/mapAttrsToList  (fn-of '(Any Any) 'Any)
   'lib/genAttrs        (fn-of '(Any Any) 'Any)
   'lib/recursiveUpdate (fn-of '(Any Any) 'Any)

   'lib/flatten         (fn-of '(Any) 'Any)
   'lib/unique          (fn-of '(Any) 'Any)
   'lib/reverseList     (fn-of '(Any) 'Any)
   'lib/take            (fn-of '(Int Any) 'Any)
   'lib/drop            (fn-of '(Int Any) 'Any)
   'lib/range           (fn-of '(Int Int) 'Any)
   'lib/imap0           (fn-of '(Any Any) 'Any)
   'lib/imap1           (fn-of '(Any Any) 'Any)

   'lib/hasPrefix       (fn-of '(String String) 'Bool)
   'lib/hasSuffix       (fn-of '(String String) 'Bool)
   'lib/removePrefix    (fn-of '(String String) 'String)
   'lib/removeSuffix    (fn-of '(String String) 'String)
   'lib/toLower         (fn-of '(String) 'String)
   'lib/toUpper         (fn-of '(String) 'String)

   ;; --- lib.types.* ---
   'lib/types.bool      (type-prim 'Any)
   'lib/types.str       (type-prim 'Any)
   'lib/types.int       (type-prim 'Any)
   'lib/types.float     (type-prim 'Any)
   'lib/types.path      (type-prim 'Any)
   'lib/types.package   (type-prim 'Any)
   'lib/types.port      (type-prim 'Any)
   'lib/types.listOf    (fn-of '(Any) 'Any)
   'lib/types.attrsOf   (fn-of '(Any) 'Any)
   'lib/types.nullOr    (fn-of '(Any) 'Any)
   'lib/types.enum      (fn-of '(Any) 'Any)
   'lib/types.submodule  (fn-of '(Any) 'Any)
   'lib/types.either    (fn-of '(Any Any) 'Any)
   'lib/types.oneOf     (fn-of '(Any) 'Any)))
