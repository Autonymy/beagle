
const BRACKET_TAG = "#%brackets";

const MAP_TAG = "#%map";

const SET_TAG = "#%set";

function bracketed_p(d) {
  return (Array.isArray(d) && (d.length > 0) && (d[0] === BRACKET_TAG));
}

function bracket_body(d) {
  return d.slice(1);
}

function map_tagged_p(d) {
  return (Array.isArray(d) && (d.length > 0) && (d[0] === MAP_TAG));
}

function map_body(d) {
  return d.slice(1);
}

function set_tagged_p(d) {
  return (Array.isArray(d) && (d.length > 0) && (d[0] === SET_TAG));
}

function set_body(d) {
  return d.slice(1);
}

function unwrap_items(d, what) {
  return (bracketed_p(d)) ? bracket_body(d) : (Array.isArray(d)) ? d : [];
}

function dot_method_sym_p(sym) {
  return ((sym.length > 1) && (sym.charAt(0) === "."));
}

function upper_case_char_p(code) {
  return ((code >= 65) && (code <= 90));
}

function static_method_sym_p(sym) {
  return (() => { const slash_pos = sym.indexOf("/"); return ((slash_pos > 0) && ((slash_pos + 1) < sym.length) && (upper_case_char_p(sym.charCodeAt(0)) || (sym.substring(0, 3) === "js/"))); })();
}

function dynamic_var_sym_p(sym) {
  return ((sym.length >= 3) && (sym.charAt(0) === "*") && (sym.charAt((sym.length - 1)) === "*"));
}

function constructor_sym_p(sym) {
  return ((sym.length > 1) && upper_case_char_p(sym.charCodeAt(0)) && (sym.charAt((sym.length - 1)) === "."));
}

function keyword_sym_p(sym) {
  return ((sym.length > 1) && (sym.charAt(0) === ":"));
}

function make_ns_decl(name) {
  return {["node"]: "ns", ["name"]: name};
}

function make_def(name, ann, value) {
  return {["node"]: "def", ["name"]: name, ["ann"]: ann, ["value"]: value};
}

function make_defonce(name, ann, value) {
  return {["node"]: "defonce", ["name"]: name, ["ann"]: ann, ["value"]: value};
}

function make_defn(name, params, rest_param, ret, body, private) {
  return {["node"]: "defn", ["name"]: name, ["params"]: params, ["rest-param"]: rest_param, ["ret"]: ret, ["body"]: body, ["private"]: private};
}

function make_defn_multi(name, arities, private) {
  return {["node"]: "defn-multi", ["name"]: name, ["arities"]: arities, ["private"]: private};
}

function make_fn(params, rest_param, ret, body) {
  return {["node"]: "fn", ["params"]: params, ["rest-param"]: rest_param, ["ret"]: ret, ["body"]: body};
}

function make_let(bindings, body) {
  return {["node"]: "let", ["bindings"]: bindings, ["body"]: body};
}

function make_if(test, then_expr, else_expr) {
  return {["node"]: "if", ["test"]: test, ["then"]: then_expr, ["else"]: else_expr};
}

function make_cond(clauses) {
  return {["node"]: "cond", ["clauses"]: clauses};
}

function make_when(test, body) {
  return {["node"]: "when", ["test"]: test, ["body"]: body};
}

function make_do(body) {
  return {["node"]: "do", ["body"]: body};
}

function make_call(fn_name, args) {
  return {["node"]: "call", ["fn"]: fn_name, ["args"]: args};
}

function make_ref(name) {
  return {["node"]: "ref", ["name"]: name};
}

function make_literal(kind, value) {
  return {["node"]: "literal", ["kind"]: kind, ["value"]: value};
}

function make_vec(items) {
  return {["node"]: "vec", ["items"]: items};
}

function make_quoted(datum) {
  return {["node"]: "quoted", ["datum"]: datum};
}

function make_unsafe(code) {
  return {["node"]: "unsafe", ["code"]: code};
}

function make_regex(pattern) {
  return {["node"]: "regex", ["pattern"]: pattern};
}

function make_loop(bindings, body) {
  return {["node"]: "loop", ["bindings"]: bindings, ["body"]: body};
}

function make_recur(args) {
  return {["node"]: "recur", ["args"]: args};
}

function make_for(clauses, body) {
  return {["node"]: "for", ["clauses"]: clauses, ["body"]: body};
}

function make_record(name, fields) {
  return {["node"]: "record", ["name"]: name, ["fields"]: fields};
}

function make_method_call(method, target, args) {
  return {["node"]: "method-call", ["method"]: method, ["target"]: target, ["args"]: args};
}

function make_static_call(class_method, args) {
  return {["node"]: "static-call", ["class-method"]: class_method, ["args"]: args};
}

function make_map(pairs) {
  return {["node"]: "map", ["pairs"]: pairs};
}

function make_set(items) {
  return {["node"]: "set", ["items"]: items};
}

function make_kw_access(kw, target, fallback) {
  return {["node"]: "kw-access", ["kw"]: kw, ["target"]: target, ["default"]: fallback};
}

function make_try(body, catches, finally_body) {
  return {["node"]: "try", ["body"]: body, ["catches"]: catches, ["finally"]: finally_body};
}

function make_catch(exception_type, name, body) {
  return {["node"]: "catch", ["exception-type"]: exception_type, ["name"]: name, ["body"]: body};
}

function make_doseq(clauses, body) {
  return {["node"]: "doseq", ["clauses"]: clauses, ["body"]: body};
}

function make_case(test, clauses, fallback) {
  return {["node"]: "case", ["test"]: test, ["clauses"]: clauses, ["default"]: fallback};
}

function make_match(target, clauses) {
  return {["node"]: "match", ["target"]: target, ["clauses"]: clauses};
}

function make_with(target, updates) {
  return {["node"]: "with", ["target"]: target, ["updates"]: updates};
}

function make_defrecord(name, fields) {
  return {["node"]: "defrecord", ["name"]: name, ["fields"]: fields};
}

function make_defenum(name, values) {
  return {["node"]: "defenum", ["name"]: name, ["values"]: values};
}

function make_defunion(name, members, type_params, member_fields) {
  return {["node"]: "defunion", ["name"]: name, ["members"]: members, ["type-params"]: type_params, ["member-fields"]: member_fields};
}

function make_deferror(name, members, member_fields) {
  return {["node"]: "deferror", ["name"]: name, ["members"]: members, ["member-fields"]: member_fields};
}

function make_defscalar(name, backing, predicates) {
  return {["node"]: "defscalar", ["name"]: name, ["backing"]: backing, ["predicates"]: predicates};
}

function make_when_let(name, expr, body) {
  return {["node"]: "when-let", ["name"]: name, ["expr"]: expr, ["body"]: body};
}

function make_if_let(name, expr, then_body, else_body) {
  return {["node"]: "if-let", ["name"]: name, ["expr"]: expr, ["then"]: then_body, ["else"]: else_body};
}

function make_when_some(name, expr, body) {
  return {["node"]: "when-some", ["name"]: name, ["expr"]: expr, ["body"]: body};
}

function make_if_some(name, expr, then_body, else_body) {
  return {["node"]: "if-some", ["name"]: name, ["expr"]: expr, ["then"]: then_body, ["else"]: else_body};
}

function make_condp(pred_fn, test_expr, clauses, fallback) {
  return {["node"]: "condp", ["pred-fn"]: pred_fn, ["test-expr"]: test_expr, ["clauses"]: clauses, ["default"]: fallback};
}

function make_dotimes(name, count_expr, body) {
  return {["node"]: "dotimes", ["name"]: name, ["count-expr"]: count_expr, ["body"]: body};
}

function make_letfn(fns, body) {
  return {["node"]: "letfn", ["fns"]: fns, ["body"]: body};
}

function make_set_bang(target, value) {
  return {["node"]: "set!", ["target"]: target, ["value"]: value};
}

function make_await(expr) {
  return {["node"]: "await", ["expr"]: expr};
}

function make_block_string(text, tag) {
  return {["node"]: "block-string", ["text"]: text, ["tag"]: tag};
}

function make_param(name, ann) {
  return {["type"]: "param", ["name"]: name, ["ann"]: ann};
}

function make_map_destructure(keys, as_name) {
  return {["type"]: "map-destructure", ["keys"]: keys, ["as"]: as_name};
}

function make_seq_destructure(names, rest_name) {
  return {["type"]: "seq-destructure", ["names"]: names, ["rest"]: rest_name};
}

function make_let_binding(name, ann, value) {
  return {["name"]: name, ["ann"]: ann, ["value"]: value};
}

function make_pat_wildcard() {
  return {["pattern"]: "wildcard"};
}

function make_pat_literal(value) {
  return {["pattern"]: "literal", ["value"]: value};
}

function make_pat_record(type_name, bindings) {
  return {["pattern"]: "record", ["type-name"]: type_name, ["bindings"]: bindings};
}

function make_pat_map(entries) {
  return {["pattern"]: "map", ["entries"]: entries};
}

function make_pat_var(name) {
  return {["pattern"]: "var", ["name"]: name};
}

function make_nix_inherit(names) {
  return {["node"]: "nix-inherit", ["names"]: names};
}

function make_nix_inherit_from(ns_expr, names) {
  return {["node"]: "nix-inherit-from", ["ns-expr"]: ns_expr, ["names"]: names};
}

function make_nix_with(ns_expr, body) {
  return {["node"]: "nix-with", ["ns-expr"]: ns_expr, ["body"]: body};
}

function make_nix_rec_attrs(pairs) {
  return {["node"]: "nix-rec-attrs", ["pairs"]: pairs};
}

function make_nix_assert(cond_expr, body) {
  return {["node"]: "nix-assert", ["cond-expr"]: cond_expr, ["body"]: body};
}

function make_nix_get_or(base, path, fallback) {
  return {["node"]: "nix-get-or", ["base"]: base, ["path"]: path, ["default"]: fallback};
}

function make_nix_has_attr(base, path) {
  return {["node"]: "nix-has-attr", ["base"]: base, ["path"]: path};
}

function make_nix_search_path(name) {
  return {["node"]: "nix-search-path", ["name"]: name};
}

function make_nix_interpolated_string(parts) {
  return {["node"]: "nix-interpolated-string", ["parts"]: parts};
}

function make_nix_multiline_string(lines) {
  return {["node"]: "nix-multiline-string", ["lines"]: lines};
}

function make_nix_path(path) {
  return {["node"]: "nix-path", ["path"]: path};
}

function make_nix_fn_set(formals, rest, at_name, body) {
  return {["node"]: "nix-fn-set", ["formals"]: formals, ["rest"]: rest, ["at-name"]: at_name, ["body"]: body};
}

function make_nix_pipe(direction, lhs, rhs) {
  return {["node"]: "nix-pipe", ["direction"]: direction, ["lhs"]: lhs, ["rhs"]: rhs};
}

function make_nix_impl(lhs, rhs) {
  return {["node"]: "nix-impl", ["lhs"]: lhs, ["rhs"]: rhs};
}

const DEFAULT_MODE = "strict";

const DEFAULT_TARGET = "clj";

const DEFAULT_NAMESPACE = "beagle.user";

function make_program(mode, namespace, target, forms, externs, requires) {
  return {["mode"]: mode, ["namespace"]: namespace, ["target"]: target, ["forms"]: forms, ["externs"]: externs, ["requires"]: requires};
}

function validate_identifier(sym) {
  return (() => { const bad_chars = ";'\"` (){}[],"; return sym.split("").every((c) => (bad_chars.indexOf(c) === -1)); })();
}

function validate_module_path(path) {
  return (path.split("").every((c) => (upper_case_char_p(c.charCodeAt(0)) || ((c.charCodeAt(0) >= 97) && (c.charCodeAt(0) <= 122)) || ((c.charCodeAt(0) >= 48) && (c.charCodeAt(0) <= 57)) || (c === ".") || (c === "_") || (c === "/") || (c === "-"))) && (path.indexOf("..") === -1));
}

