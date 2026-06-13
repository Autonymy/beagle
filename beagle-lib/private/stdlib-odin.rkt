#lang racket/base

(require "types.rkt"
         "stdlib-helpers.rkt")

(define STDLIB-ODIN
  (hash
   ;; type casts — (f32 x), (u8 x), etc.
   'f32   (fn-of '(Number) 'F32)
   'f64   (fn-of '(Number) 'Float)
   'i8    (fn-of '(Number) 'Int)
   'i16   (fn-of '(Number) 'Int)
   'i32   (fn-of '(Number) 'Int)
   'i64   (fn-of '(Number) 'Int)
   'u8    (fn-of '(Number) 'Int)
   'u16   (fn-of '(Number) 'Int)
   'u32   (fn-of '(Number) 'Int)
   'u64   (fn-of '(Number) 'Int)
   ;; math.* functions — the emitter lowers these to math.cos etc.
   ;; Odin math operates on concrete float types; F32 is the primary
   ;; game float. Int/Float coerce to F32 per the type-compat rules.
   'cos    (fn-of '(F32) 'F32)
   'sin    (fn-of '(F32) 'F32)
   'tan    (fn-of '(F32) 'F32)
   'acos   (fn-of '(F32) 'F32)
   'asin   (fn-of '(F32) 'F32)
   'atan   (fn-of '(F32) 'F32)
   'atan2  (fn-of '(F32 F32) 'F32)
   'sqrt   (fn-of '(F32) 'F32)
   'floor  (fn-of '(F32) 'F32)
   'ceil   (fn-of '(F32) 'F32)
   'pow    (fn-of '(F32 F32) 'F32)
   'log    (fn-of '(F32) 'F32)
   'log2   (fn-of '(F32) 'F32)
   'log10  (fn-of '(F32) 'F32)))

(provide STDLIB-ODIN)
