
const BRACKET_TAG = "#%brackets";

const MAP_TAG = "#%map";

const SET_TAG = "#%set";

const META_FORMS = ["ns", "define-mode", "define-target", "define-macro", "declare-extern", "require", "import"];

const THREADING_FORMS = ["->", "->>", "cond->", "cond->>", "some->", "some->>", "as->"];

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

function unwrap_items(d) {
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

function constructor_sym_p(sym) {
  return ((sym.length > 1) && upper_case_char_p(sym.charCodeAt(0)) && (sym.charAt((sym.length - 1)) === "."));
}

function keyword_sym_p(sym) {
  return ((sym.length > 1) && (sym.charAt(0) === ":"));
}

function dynamic_var_sym_p(sym) {
  return ((sym.length >= 3) && (sym.charAt(0) === "*") && (sym.charAt((sym.length - 1)) === "*"));
}

const PARAMETRIC_CTORS = ["Vec", "List", "Set", "Map", "Promise"];

const CLJ_ALIASES = {["Long"]: "Int", ["Double"]: "Float", ["Boolean"]: "Bool", ["Integer"]: "Int"};

const user_parametric = {};

function make_prim(name) {
  return {["kind"]: "prim", ["name"]: name};
}

function make_fn_type(params, rest_type, ret) {
  return {["kind"]: "fn", ["params"]: params, ["rest"]: rest_type, ["ret"]: ret};
}

function make_app(ctor, args) {
  return {["kind"]: "app", ["name"]: ctor, ["args"]: args};
}

function make_union(members) {
  return {["kind"]: "union", ["members"]: members};
}

function make_var(name) {
  return {["kind"]: "var", ["name"]: name};
}

function make_poly(vars, body, bounds) {
  return {["kind"]: "poly", ["vars"]: vars, ["body"]: body, ["bounds"]: bounds};
}

function parse_fn_type_items(items) {
  return (() => { const arrow_pos = items.indexOf("->"); return ((arrow_pos === -1) ? make_prim("Any") : (() => { const before = items.slice(0, arrow_pos); const after = items.slice((arrow_pos + 1)); return ((after.length !== 1) ? make_prim("Any") : (() => { const amp_pos = before.indexOf("&"); return ((amp_pos > -1) ? make_fn_type(before.slice(0, amp_pos).map(parse_type), parse_type(before.slice((amp_pos + 1))[0]), parse_type(after[0])) : make_fn_type(before.map(parse_type), null, parse_type(after[0]))); })()); })()); })();
}

function parse_type(t) {
  return ((Array.isArray(t) && (t.length > 0) && (t[0] === BRACKET_TAG))) ? parse_fn_type_items(t.slice(1)) : ((Array.isArray(t) && (t.length === 3) && (t[0] === "forall"))) ? (() => { const vars_form = t[1]; const raw_vars = ((Array.isArray(vars_form) && (vars_form.length > 0) && (vars_form[0] === BRACKET_TAG)) ? vars_form.slice(1) : vars_form); const vars = raw_vars.filter((v) => (typeof v === 'string')); return make_poly(vars, parse_type(t[2]), null); })() : ((Array.isArray(t) && (t.length > 1) && (t[0] === "U"))) ? make_union(t.slice(1).map(parse_type)) : ((Array.isArray(t) && (t.length > 0) && (typeof t[0] === 'string') && (PARAMETRIC_CTORS.includes(t[0]) || user_parametric[t[0]]))) ? make_app(t[0], t.slice(1).map(parse_type)) : (((typeof t === 'string') && (t.length > 1) && (t.charAt((t.length - 1)) === "?"))) ? (() => { const base = t.substring(0, (t.length - 1)); return make_union([parse_type(base), make_prim("Nil")]); })() : (((typeof t === 'string') && (t === "Number"))) ? make_union([make_prim("Int"), make_prim("Float")]) : (((typeof t === 'string') && (!(CLJ_ALIASES[t] == null)))) ? make_prim(CLJ_ALIASES[t]) : ((typeof t === 'string')) ? make_prim(t) : make_prim("Any");
}

function make_literal(kind, value) {
  return {["node"]: "literal", ["kind"]: kind, ["value"]: value};
}

function make_ref(name) {
  return {["node"]: "ref", ["name"]: name};
}

function make_def(name, ann, value) {
  return {["node"]: "def", ["name"]: name, ["ann"]: ann, ["value"]: value};
}

function make_defonce(name, ann, value) {
  return {["node"]: "defonce", ["name"]: name, ["ann"]: ann, ["value"]: value};
}

function make_defn(name, params, rest_param, ret, body, priv) {
  return {["node"]: "defn", ["name"]: name, ["params"]: params, ["rest"]: ((rest_param == null) ? false : rest_param), ["ret"]: ret, ["body"]: body, ["private"]: priv};
}

function make_defn_multi(name, arities, priv) {
  return {["node"]: "defn-multi", ["name"]: name, ["arities"]: arities, ["private"]: priv};
}

function make_fn(params, rest_param, ret, body) {
  return {["node"]: "fn", ["params"]: params, ["rest"]: ((rest_param == null) ? false : rest_param), ["ret"]: ret, ["body"]: body};
}

function make_let(bindings, body) {
  return {["node"]: "let", ["bindings"]: bindings, ["body"]: body};
}

function make_if(test, then_expr, else_expr) {
  return {["node"]: "if", ["cond"]: test, ["then"]: then_expr, ["else"]: else_expr};
}

function make_cond(clauses) {
  return {["node"]: "cond", ["clauses"]: clauses};
}

function make_when(test, body) {
  return {["node"]: "when", ["cond"]: test, ["body"]: body};
}

function make_do(body) {
  return {["node"]: "do", ["body"]: body};
}

function make_call(fn_expr, args) {
  return {["node"]: "call", ["fn"]: fn_expr, ["args"]: args};
}

function make_vec(items) {
  return {["node"]: "vec", ["items"]: items};
}

function make_map(pairs) {
  return {["node"]: "map", ["pairs"]: pairs};
}

function make_set_form(items) {
  return {["node"]: "set", ["items"]: items};
}

function make_quoted(datum) {
  return {["node"]: "quoted", ["datum"]: datum};
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

function make_method_call(method, target, args) {
  return {["node"]: "method-call", ["method"]: method, ["target"]: target, ["args"]: args};
}

function make_static_call(class_method, args) {
  return {["node"]: "static-call", ["name"]: class_method, ["args"]: args};
}

function make_kw_access(kw, target, fallback) {
  return {["node"]: "kw-access", ["kw"]: kw, ["target"]: target, ["default"]: fallback};
}

function make_try(body, catches, finally_body) {
  return {["node"]: "try", ["body"]: body, ["catches"]: catches, ["finally"]: finally_body};
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
  return {["node"]: "record", ["name"]: name, ["fields"]: fields};
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

function make_defscalar(name, backing) {
  return {["node"]: "defscalar", ["name"]: name, ["backing"]: backing};
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
  return {["node"]: "condp", ["pred"]: pred_fn, ["test"]: test_expr, ["clauses"]: clauses, ["default"]: fallback};
}

function make_dotimes(name, count_expr, body) {
  return {["node"]: "dotimes", ["name"]: name, ["count"]: count_expr, ["body"]: body};
}

function make_doseq(clauses, body) {
  return {["node"]: "doseq", ["clauses"]: clauses, ["body"]: body};
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

function make_new(class_name, args) {
  return {["node"]: "new", ["class"]: class_name, ["args"]: args};
}

function make_unsafe_raw(code) {
  return {["node"]: "unsafe-raw", ["code"]: code};
}

function string_datum_p(d) {
  return ((typeof d === 'string') || (Array.isArray(d) && (d.length === 2) && (d[0] === "#%string")));
}

function extract_string(d) {
  return ((typeof d === 'string') ? d : d[1]);
}

function make_regex(pattern) {
  return {["node"]: "regex", ["pattern"]: pattern};
}

function make_dynamic_var(name) {
  return {["node"]: "dynamic-var", ["name"]: name};
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
  return {["type"]: "wildcard"};
}

function make_pat_literal(value) {
  return {["type"]: "literal", ["value"]: value};
}

function make_pat_record(type_name, bindings) {
  return {["type"]: "record", ["name"]: type_name, ["bindings"]: bindings};
}

function make_pat_map(entries) {
  return {["type"]: "map", ["entries"]: entries};
}

function make_pat_var(name) {
  return {["type"]: "var", ["name"]: name};
}

const gensym_counter = {["n"]: 0};

function gensym(prefix) {
  return (() => { const n = gensym_counter["n"]; (gensym_counter["n"] = (n + 1));
return ("".concat(prefix, "__g", ("".concat(n)))); })();
}

function datum__gtjson(d) {
  return ((typeof d === 'string')) ? d : ((typeof d === 'number')) ? d : ((typeof d === 'boolean')) ? d : ((d == null)) ? null : ((Array.isArray(d) && (d.length === 2) && (d[0] === "#%string"))) ? d[1] : (Array.isArray(d)) ? d.map(datum__gtjson) : ("".concat(d));
}

function annotation_marker_p(sym) {
  return (sym === ":");
}

function map_destructure_form_p(item) {
  return (map_tagged_p(item) && (() => { const body = map_body(item); return ((body.length >= 2) && (body[0] === ":keys") && bracketed_p(body[1])); })());
}

function parse_map_destructure(item) {
  return (() => { const body = map_body(item); const keys_bracket = body[1]; const key_names = bracket_body(keys_bracket); const as_name = (((body.length >= 4) && (body[2] === ":as") && (typeof body[3] === 'string')) ? body[3] : null); return make_map_destructure(key_names, as_name); })();
}

function parse_seq_destructure(item) {
  return (() => { const body = bracket_body(item); const result = {["names"]: [], ["rest"]: null}; return (() => { function walk(i) { return ((i >= body.length)) ? result : ((body[i] === "&")) ? (() => { ((((i + 1) === (body.length - 1)) && (typeof body[(i + 1)] === 'string')) ? (result["rest"] = body[(i + 1)]) : null);
return result; })() : ((typeof body[i] === 'string')) ? (() => { result["names"].push(body[i]);
return walk((i + 1)); })() : result; } walk(0);
return make_seq_destructure(result["names"], result["rest"]); })(); })();
}

function parse_params(params_form) {
  return (() => { const items = unwrap_items(params_form); const state = {["fixed"]: [], ["rest-param"]: null}; return (() => { function process_items(i) { return ((i >= items.length)) ? null : ((items[i] === "&")) ? (((i + 1) < items.length) ? (() => { const after_amp = items[(i + 1)]; return ((Array.isArray(after_amp) && (after_amp.length === 3) && (typeof after_amp[0] === 'string') && annotation_marker_p(after_amp[1]))) ? (state["rest-param"] = make_param(after_amp[0], parse_type(after_amp[2]))) : ((typeof after_amp === 'string')) ? (state["rest-param"] = make_param(after_amp, null)) : null; })() : null) : (() => { const item = items[i]; (bracketed_p(item)) ? state["fixed"].push(parse_seq_destructure(item)) : (map_destructure_form_p(item)) ? state["fixed"].push(parse_map_destructure(item)) : ((Array.isArray(item) && (item.length === 3) && (typeof item[0] === 'string') && annotation_marker_p(item[1]))) ? state["fixed"].push(make_param(item[0], parse_type(item[2]))) : ((typeof item === 'string')) ? state["fixed"].push(make_param(item, null)) : null;
return process_items((i + 1)); })(); } process_items(0);
return {["params"]: state["fixed"], ["rest-param"]: state["rest-param"]}; })(); })();
}

function parse_let_bindings(b) {
  return (() => { const items = unwrap_items(b); const result = []; return (() => { function walk(i) { return ((i >= items.length)) ? null : ((((i + 1) < items.length) && Array.isArray(items[i]) && (items[i].length === 3) && (typeof items[i][0] === 'string') && annotation_marker_p(items[i][1]))) ? (() => { result.push(make_let_binding(items[i][0], parse_type(items[i][2]), parse_expr(items[(i + 1)])));
return walk((i + 2)); })() : ((((i + 1) < items.length) && map_destructure_form_p(items[i]))) ? (() => { result.push(make_let_binding(parse_map_destructure(items[i]), null, parse_expr(items[(i + 1)])));
return walk((i + 2)); })() : ((((i + 1) < items.length) && bracketed_p(items[i]))) ? (() => { result.push(make_let_binding(parse_seq_destructure(items[i]), null, parse_expr(items[(i + 1)])));
return walk((i + 2)); })() : ((((i + 1) < items.length) && (typeof items[i] === 'string'))) ? (() => { result.push(make_let_binding(items[i], null, parse_expr(items[(i + 1)])));
return walk((i + 2)); })() : null; } walk(0);
return result; })(); })();
}

function parse_record_fields(f) {
  return (() => { const items = unwrap_items(f); return items.filter((item) => (Array.isArray(item) && (item.length === 3) && (typeof item[0] === 'string') && annotation_marker_p(item[1]))).map((item) => make_param(item[0], parse_type(item[2]))); })();
}

function parse_cond_clauses(clauses) {
  return ((clauses.length === 0)) ? [] : (bracketed_p(clauses[0])) ? clauses.map((c) => (() => { const items = (bracketed_p(c) ? bracket_body(c) : c); return ((Array.isArray(items) && (items.length > 1)) ? {["test"]: parse_expr(items[0]), ["body"]: items.slice(1).map(parse_expr)} : {["test"]: parse_expr(c), ["body"]: []}); })()) : (() => { const result = []; return (() => { function walk(i) { return ((i >= clauses.length)) ? null : (((i + 1) < clauses.length)) ? (() => { result.push({["test"]: parse_expr(clauses[i]), ["body"]: [parse_expr(clauses[(i + 1)])]});
return walk((i + 2)); })() : null; } walk(0);
return result; })(); })();
}

function parse_for_clauses(b) {
  return (() => { const items = unwrap_items(b); const result = []; return (() => { function walk(i) { return ((i >= items.length)) ? null : ((((i + 1) < items.length) && (items[i] === ":when"))) ? (() => { result.push({["type"]: "when", ["test"]: parse_expr(items[(i + 1)])});
return walk((i + 2)); })() : ((((i + 1) < items.length) && (items[i] === ":let"))) ? (() => { result.push({["type"]: "let", ["bindings"]: parse_let_bindings(items[(i + 1)])});
return walk((i + 2)); })() : ((((i + 1) < items.length) && bracketed_p(items[i]))) ? (() => { result.push({["type"]: "binding", ["name"]: parse_seq_destructure(items[i]), ["expr"]: parse_expr(items[(i + 1)])});
return walk((i + 2)); })() : ((((i + 1) < items.length) && map_destructure_form_p(items[i]))) ? (() => { result.push({["type"]: "binding", ["name"]: parse_map_destructure(items[i]), ["expr"]: parse_expr(items[(i + 1)])});
return walk((i + 2)); })() : ((((i + 1) < items.length) && (typeof items[i] === 'string'))) ? (() => { result.push({["type"]: "binding", ["name"]: items[i], ["expr"]: parse_expr(items[(i + 1)])});
return walk((i + 2)); })() : null; } walk(0);
return result; })(); })();
}

function parse_try_form(rest_items) {
  return (() => { const state = {["phase"]: "body", ["body-forms"]: [], ["catch-forms"]: [], ["finally-body"]: null}; return (() => { function walk(i) { return ((i >= rest_items.length)) ? null : (() => { const item = rest_items[i]; return ((Array.isArray(item) && (item.length > 0) && (item[0] === "catch"))) ? (() => { (state["phase"] = "catch");
((item.length >= 4) ? state["catch-forms"].push({["type"]: item[1], ["name"]: item[2], ["body"]: item.slice(3).map(parse_expr)}) : null);
return walk((i + 1)); })() : ((Array.isArray(item) && (item.length > 0) && (item[0] === "finally"))) ? (() => { (state["phase"] = "finally");
(state["finally-body"] = item.slice(1).map(parse_expr));
return walk((i + 1)); })() : ((state["phase"] === "body")) ? (() => { state["body-forms"].push(parse_expr(item));
return walk((i + 1)); })() : walk((i + 1)); })(); } walk(0);
return make_try(state["body-forms"], state["catch-forms"], state["finally-body"]); })(); })();
}

function parse_case_form(test_datum, clauses) {
  return (() => { const test = parse_expr(test_datum); return ((clauses.length === 0)) ? make_case(test, [], null) : (((clauses.length % 2) === 1)) ? (() => { const pairs = clauses.slice(0, (clauses.length - 1)); const fallback = parse_expr(clauses[(clauses.length - 1)]); return make_case(test, parse_case_pairs(pairs), fallback); })() : make_case(test, parse_case_pairs(clauses), null); })();
}

function parse_case_pairs(items) {
  return (() => { const result = []; return (() => { function walk(i) { return ((i >= items.length)) ? null : (((i + 1) < items.length)) ? (() => { result.push({["value"]: datum__gtjson(items[i]), ["body"]: [parse_expr(items[(i + 1)])]});
return walk((i + 2)); })() : null; } walk(0);
return result; })(); })();
}

function parse_match_form(target_datum, clauses) {
  return make_match(parse_expr(target_datum), clauses.map((c) => (() => { const items = (bracketed_p(c) ? bracket_body(c) : c); return ((Array.isArray(items) && (items.length >= 2)) ? {["pattern"]: parse_pattern(items[0]), ["body"]: items.slice(1).map(parse_expr)} : {["pattern"]: parse_pattern(c), ["body"]: []}); })()));
}

function parse_pattern(p) {
  return ((p === "_")) ? make_pat_wildcard() : ((p === "nil")) ? make_pat_literal(null) : (((typeof p === 'string') && keyword_sym_p(p))) ? make_pat_literal(p) : ((typeof p === 'number')) ? make_pat_literal(p) : ((typeof p === 'boolean')) ? make_pat_literal(p) : ((Array.isArray(p) && (p.length > 0) && (p[0] === MAP_TAG))) ? parse_map_pattern(p.slice(1)) : ((Array.isArray(p) && (p.length > 0) && (typeof p[0] === 'string') && (p[0].length > 0) && upper_case_char_p(p[0].charCodeAt(0)))) ? make_pat_record(p[0], p.slice(1).map((b) => (() => { const r = {["name"]: b}; return r; })())) : ((typeof p === 'string')) ? make_pat_var(p) : make_pat_literal(datum__gtjson(p));
}

function parse_map_pattern(entries) {
  return (() => { const result = []; return (() => { function walk(i) { return ((i >= entries.length)) ? null : ((((i + 1) < entries.length) && (typeof entries[i] === 'string') && keyword_sym_p(entries[i]))) ? (() => { result.push({["key"]: entries[i], ["name"]: entries[(i + 1)]});
return walk((i + 2)); })() : walk((i + 1)); } walk(0);
return make_pat_map(result); })(); })();
}

function parse_with_form(target_datum, updates) {
  return make_with(parse_expr(target_datum), updates.map((u) => ((bracketed_p(u) && (bracket_body(u).length >= 2)) ? (() => { const items = bracket_body(u); const kw = items[0]; return {["field"]: kw, ["value"]: parse_expr(items[1])}; })() : {["field"]: "", ["value"]: null})));
}

function parse_letfn_fns(form) {
  return (() => { const items = unwrap_items(form); return items.map((item) => ((Array.isArray(item) && (item.length >= 3) && (typeof item[0] === 'string')) ? (() => { const name = item[0]; const params_form = item[1]; const rest_forms = item.slice(2); const parsed_params = parse_params(params_form); return (((rest_forms.length >= 2) && annotation_marker_p(rest_forms[0]))) ? {["name"]: name, ["params"]: parsed_params["params"], ["rest"]: parsed_params["rest-param"], ["ret"]: parse_type(rest_forms[1]), ["body"]: rest_forms.slice(2).map(parse_expr)} : {["name"]: name, ["params"]: parsed_params["params"], ["rest"]: parsed_params["rest-param"], ["ret"]: null, ["body"]: rest_forms.map(parse_expr)}; })() : null)); })();
}

function parse_condp_form(pred_datum, test_datum, clause_datums) {
  return (() => { const pred_expr = parse_expr(pred_datum); const test_expr = parse_expr(test_datum); const state = {["pairs"]: [], ["fallback"]: null}; return (() => { function walk(i) { return ((i >= clause_datums.length)) ? null : ((i === (clause_datums.length - 1))) ? (state["fallback"] = parse_expr(clause_datums[i])) : (((i + 1) < clause_datums.length)) ? (() => { state["pairs"].push({["test"]: parse_expr(clause_datums[i]), ["body"]: parse_expr(clause_datums[(i + 1)])});
return walk((i + 2)); })() : null; } (((clause_datums.length % 2) === 1) ? walk(0) : walk(0));
return make_condp(pred_expr, test_expr, state["pairs"], state["fallback"]); })(); })();
}

function parse_cond_let_binding(b) {
  return (() => { const items = unwrap_items(b); return (((items.length === 2) && (typeof items[0] === 'string')) ? {["name"]: items[0], ["expr"]: parse_expr(items[1])} : {["name"]: "_", ["expr"]: null}); })();
}

function multi_arity_form_p(d) {
  return (Array.isArray(d) && (!bracketed_p(d)) && (d.length > 0) && Array.isArray(d[0]) && bracketed_p(d[0]));
}

function parse_arity_clause(clause) {
  return (() => { const params_form = clause[0]; const rest_forms = clause.slice(1); const parsed_params = parse_params(params_form); return (((rest_forms.length >= 2) && annotation_marker_p(rest_forms[0]))) ? {["params"]: parsed_params["params"], ["rest"]: parsed_params["rest-param"], ["ret"]: parse_type(rest_forms[1]), ["body"]: rest_forms.slice(2).map(parse_expr)} : {["params"]: parsed_params["params"], ["rest"]: parsed_params["rest-param"], ["ret"]: null, ["body"]: rest_forms.map(parse_expr)}; })();
}

function thread_step_insert(val, step, position) {
  return (Array.isArray(step) ? ((position === "first") ? [step[0]].concat([val], step.slice(1)) : step.concat([val])) : [step, val]);
}

function expand_thread_first(init, steps) {
  return steps.reduce((acc, step) => thread_step_insert(acc, step, "first"), init);
}

function expand_thread_last(init, steps) {
  return steps.reduce((acc, step) => thread_step_insert(acc, step, "last"), init);
}

function expand_cond_thread(kind, init, clauses) {
  return (() => { const pairs = []; const sym = Symbol("ct"); return (() => { function collect_pairs(i) { return (((i + 1) >= clauses.length)) ? null : (() => { pairs.push([clauses[i], clauses[(i + 1)]]);
return collect_pairs((i + 2)); })(); } collect_pairs(0);
return (() => { const pos = ((kind === "cond->") ? "first" : "last"); const inner = pairs.reduceRight((acc, pair) => ["let", [BRACKET_TAG, sym, ["if", pair[0], thread_step_insert(sym, pair[1], pos), sym]], acc], sym); return ["let", [BRACKET_TAG, sym, init], inner]; })(); })(); })();
}

function expand_some_thread(kind, init, steps) {
  return (() => { const pos = ((kind === "some->") ? "first" : "last"); const sym = Symbol("st"); return steps.reduce((acc, step) => ["let", [BRACKET_TAG, sym, acc], ["if", ["some?", sym], thread_step_insert(sym, step, pos), "nil"]], init); })();
}

function expand_as_thread(init, name, steps) {
  return steps.reduce((acc, step) => ["let", [BRACKET_TAG, name, acc], step], init);
}

function parse_simple_defunion(name, raw_members) {
  return (() => { const mnames = []; const mf_hash = {}; const has_fields = {["v"]: false}; raw_members.forEach((m) => ((Array.isArray(m) && (m.length > 0)) ? (() => { mnames.push(m[0]);
return (((m.length >= 2) && Array.isArray(m[1])) ? (() => { (mf_hash[m[0]] = parse_record_fields(m[1]));
return (has_fields["v"] = true); })() : null); })() : (() => { mnames.push(m);
return null; })()));
return make_defunion(name, mnames, null, (has_fields["v"] ? mf_hash : null)); })();
}

function parse_parametric_defunion(name, type_vars, member_defs) {
  return (() => { const mnames = []; const mf_hash = {}; member_defs.forEach((md) => ((Array.isArray(md) && (md.length >= 2) && (typeof md[0] === 'string')) ? (() => { mnames.push(md[0]);
return (mf_hash[md[0]] = parse_record_fields(md[1])); })() : null));
return make_defunion(name, mnames, type_vars, mf_hash); })();
}

function parse_deferror_form(name, member_defs) {
  return (() => { const mnames = []; const mf_hash = {}; member_defs.forEach((md) => ((typeof md === 'string')) ? (() => { mnames.push(md);
return (mf_hash[md] = []); })() : ((Array.isArray(md) && (md.length >= 2) && (typeof md[0] === 'string'))) ? (() => { mnames.push(md[0]);
return (mf_hash[md[0]] = parse_record_fields(md[1])); })() : null);
return make_deferror(name, mnames, mf_hash); })();
}

function fmt_split_template(text) {
  return (() => { const result = []; const len = text.length; const state = {["i"]: 0, ["start"]: 0}; return (() => { function walk() { return (() => { const i = state["i"]; return ((i >= len)) ? (() => { const tail = text.substring(state["start"], len); return (() => { if ((tail.length > 0)) { return result.push(tail); } })(); })() : ((((i + 1) < len) && (text.charAt(i) === "$") && (text.charAt((i + 1)) === "{"))) ? (() => { const prefix = text.substring(state["start"], i); (() => { if ((prefix.length > 0)) { return result.push(prefix); } })();
return (() => { const close_pos = fmt_find_close(text, (i + 2)); return ((close_pos > -1) ? (() => { const expr_str = text.substring((i + 2), close_pos).trim(); const datum = rereadDatum(expr_str, 0)["value"]; result.push(["#%expr", datum]);
(state["i"] = (close_pos + 1));
(state["start"] = (close_pos + 1));
return walk(); })() : (() => { (state["i"] = (i + 1));
return walk(); })()); })(); })() : (() => { (state["i"] = (i + 1));
return walk(); })(); })(); } walk();
return result; })(); })();
}

function fmt_find_close(text, start) {
  return (() => { const len = text.length; const state = {["i"]: start, ["depth"]: 1}; return (() => { function walk() { return (() => { const i = state["i"]; return ((i >= len)) ? -1 : ((text.charAt(i) === "}")) ? ((state["depth"] === 1) ? i : (() => { (state["depth"] = (state["depth"] - 1));
(state["i"] = (i + 1));
return walk(); })()) : ((text.charAt(i) === "{")) ? (() => { (state["depth"] = (state["depth"] + 1));
(state["i"] = (i + 1));
return walk(); })() : (() => { (state["i"] = (i + 1));
return walk(); })(); })(); } return walk(); })(); })();
}

function fmt_unwrap_part(p) {
  return ((Array.isArray(p) && (p[0] === "#%expr")) ? p[1] : ["#%string", p]);
}

function expand_fmt(text) {
  return (() => { const parts = fmt_split_template(text); return ((parts.length === 0)) ? ["#%string", ""] : (((parts.length === 1) && (typeof parts[0] === 'string'))) ? ["#%string", parts[0]] : ["str"].concat(parts.map(fmt_unwrap_part)); })();
}

function parse_expr(d) {
  return ((d == null)) ? make_literal("nil", null) : ((typeof d === 'boolean')) ? make_literal("bool", d) : (((typeof d === 'number') && (d !== Math.floor(d)))) ? make_literal("float", d) : ((typeof d === 'number')) ? make_literal("number", d) : ((Array.isArray(d) && (d.length === 2) && (d[0] === "#%string"))) ? make_literal("string", d[1]) : (((typeof d === 'string') && (d === "nil"))) ? make_literal("nil", null) : (((typeof d === 'string') && keyword_sym_p(d))) ? make_literal("keyword", d.substring(1)) : (((typeof d === 'string') && dynamic_var_sym_p(d))) ? make_dynamic_var(d) : ((typeof d === 'string')) ? make_ref(d) : ((Array.isArray(d) && (d.length === 2) && (d[0] === "#%regex"))) ? make_regex(d[1]) : (bracketed_p(d)) ? make_vec(bracket_body(d).map(parse_expr)) : (map_tagged_p(d)) ? parse_map_literal(map_body(d)) : (set_tagged_p(d)) ? make_set_form(set_body(d).map(parse_expr)) : ((Array.isArray(d) && (d.length === 2) && (d[0] === "quote"))) ? make_quoted(datum__gtjson(d[1])) : ((Array.isArray(d) && (d.length > 0))) ? parse_list_form(d) : make_literal("nil", null);
}

function parse_map_literal(items) {
  return (() => { const pairs = []; return (() => { function walk(i) { return ((i >= items.length)) ? null : (((i + 1) < items.length)) ? (() => { pairs.push({["key"]: parse_expr(items[i]), ["val"]: parse_expr(items[(i + 1)])});
return walk((i + 2)); })() : null; } walk(0);
return make_map(pairs); })(); })();
}

function parse_list_form(d) {
  return (() => { const head = d[0]; const rest_items = d.slice(1); return (((head === "unsafe") && (rest_items.length === 1) && string_datum_p(rest_items[0]))) ? (() => { process.stderr.write("beagle: (unsafe \"...\") is ambiguous. Use (unsafe-js \"...\"), (unsafe-clj \"...\"), etc.\n");
return make_unsafe_raw(extract_string(rest_items[0])); })() : ((head.startsWith("unsafe-") && (rest_items.length === 1) && string_datum_p(rest_items[0]))) ? make_unsafe_raw(extract_string(rest_items[0])) : (((head === "def") && (rest_items.length === 3) && annotation_marker_p(rest_items[1]))) ? make_def(rest_items[0], parse_type(rest_items[2]), null) : (((head === "def") && (rest_items.length >= 3) && annotation_marker_p(rest_items[1]))) ? make_def(rest_items[0], parse_type(rest_items[2]), parse_expr(rest_items[3])) : (((head === "def") && (rest_items.length === 2))) ? make_def(rest_items[0], null, parse_expr(rest_items[1])) : (((head === "defonce") && (rest_items.length >= 3) && annotation_marker_p(rest_items[1]))) ? make_defonce(rest_items[0], parse_type(rest_items[2]), parse_expr(rest_items[3])) : (((head === "defonce") && (rest_items.length === 2))) ? make_defonce(rest_items[0], null, parse_expr(rest_items[1])) : (((head === "defn") && (rest_items.length >= 2) && (typeof rest_items[0] === 'string') && multi_arity_form_p(rest_items[1]))) ? make_defn_multi(rest_items[0], rest_items.slice(1).map(parse_arity_clause), false) : (((head === "defn") && (rest_items.length >= 4) && (typeof rest_items[0] === 'string') && annotation_marker_p(rest_items[2]))) ? (() => { const parsed_params = parse_params(rest_items[1]); return make_defn(rest_items[0], parsed_params["params"], parsed_params["rest-param"], parse_type(rest_items[3]), rest_items.slice(4).map(parse_expr), false); })() : (((head === "defn") && (rest_items.length >= 2) && (typeof rest_items[0] === 'string'))) ? (() => { const parsed_params = parse_params(rest_items[1]); return make_defn(rest_items[0], parsed_params["params"], parsed_params["rest-param"], null, rest_items.slice(2).map(parse_expr), false); })() : (((head === "defn-") && (rest_items.length >= 2) && (typeof rest_items[0] === 'string'))) ? (() => { const name = rest_items[0]; const after_name = rest_items.slice(1); return (((after_name.length >= 1) && multi_arity_form_p(after_name[0])) ? make_defn_multi(name, after_name.map(parse_arity_clause), true) : (((after_name.length >= 3) && annotation_marker_p(after_name[1])) ? (() => { const parsed_params = parse_params(after_name[0]); return make_defn(name, parsed_params["params"], parsed_params["rest-param"], parse_type(after_name[2]), after_name.slice(3).map(parse_expr), true); })() : (() => { const parsed_params = parse_params(after_name[0]); return make_defn(name, parsed_params["params"], parsed_params["rest-param"], null, after_name.slice(1).map(parse_expr), true); })())); })() : (((head === "defrecord") && (rest_items.length === 2))) ? make_defrecord(rest_items[0], parse_record_fields(rest_items[1])) : (((head === "defenum") && (rest_items.length >= 1))) ? make_defenum(rest_items[0], rest_items.slice(1)) : (((head === "defunion") && (rest_items.length >= 1) && Array.isArray(rest_items[0]))) ? (() => { const name_form = rest_items[0]; return (((name_form.length >= 2) && (typeof name_form[0] === 'string')) ? parse_parametric_defunion(name_form[0], name_form.slice(1), rest_items.slice(1)) : parse_simple_defunion(rest_items[0], rest_items.slice(1))); })() : (((head === "defunion") && (rest_items.length >= 1) && (typeof rest_items[0] === 'string'))) ? parse_simple_defunion(rest_items[0], rest_items.slice(1)) : (((head === "deferror") && (rest_items.length >= 1))) ? parse_deferror_form(rest_items[0], rest_items.slice(1)) : (((head === "defscalar") && (rest_items.length >= 2))) ? make_defscalar(rest_items[0], parse_type(rest_items[1])) : (((head === "fn") && (rest_items.length >= 3) && annotation_marker_p(rest_items[1]))) ? (() => { const parsed_params = parse_params(rest_items[0]); return make_fn(parsed_params["params"], parsed_params["rest-param"], parse_type(rest_items[2]), rest_items.slice(3).map(parse_expr)); })() : (((head === "fn") && (rest_items.length >= 1))) ? (() => { const parsed_params = parse_params(rest_items[0]); return make_fn(parsed_params["params"], parsed_params["rest-param"], null, rest_items.slice(1).map(parse_expr)); })() : (((head === "let") && (rest_items.length >= 1))) ? make_let(parse_let_bindings(rest_items[0]), rest_items.slice(1).map(parse_expr)) : (((head === "letfn") && (rest_items.length >= 1))) ? make_letfn(parse_letfn_fns(rest_items[0]), rest_items.slice(1).map(parse_expr)) : (((head === "loop") && (rest_items.length >= 1))) ? make_loop(parse_let_bindings(rest_items[0]), rest_items.slice(1).map(parse_expr)) : ((head === "recur")) ? make_recur(rest_items.map(parse_expr)) : (((head === "await") && (rest_items.length === 1))) ? make_await(parse_expr(rest_items[0])) : (((head === "set!") && (rest_items.length === 2))) ? make_set_bang(parse_expr(rest_items[0]), parse_expr(rest_items[1])) : (((head === "for") && (rest_items.length >= 1))) ? make_for(parse_for_clauses(rest_items[0]), rest_items.slice(1).map(parse_expr)) : (((head === "if") && (rest_items.length === 3))) ? make_if(parse_expr(rest_items[0]), parse_expr(rest_items[1]), parse_expr(rest_items[2])) : (((head === "if") && (rest_items.length === 2))) ? make_if(parse_expr(rest_items[0]), parse_expr(rest_items[1]), null) : (((head === "when") && (rest_items.length >= 1))) ? make_when(parse_expr(rest_items[0]), rest_items.slice(1).map(parse_expr)) : (((head === "when-not") && (rest_items.length >= 1))) ? make_when(make_call(make_ref("not"), [parse_expr(rest_items[0])]), rest_items.slice(1).map(parse_expr)) : (((head === "if-not") && (rest_items.length >= 2))) ? make_if(make_call(make_ref("not"), [parse_expr(rest_items[0])]), parse_expr(rest_items[1]), ((rest_items.length >= 3) ? parse_expr(rest_items[2]) : null)) : (((head === "when-let") && (rest_items.length >= 1))) ? (() => { const binding = parse_cond_let_binding(rest_items[0]); return make_when_let(binding["name"], binding["expr"], rest_items.slice(1).map(parse_expr)); })() : (((head === "if-let") && (rest_items.length >= 2))) ? (() => { const binding = parse_cond_let_binding(rest_items[0]); return make_if_let(binding["name"], binding["expr"], parse_expr(rest_items[1]), ((rest_items.length >= 3) ? parse_expr(rest_items[2]) : null)); })() : (((head === "when-some") && (rest_items.length >= 1))) ? (() => { const binding = parse_cond_let_binding(rest_items[0]); return make_when_some(binding["name"], binding["expr"], rest_items.slice(1).map(parse_expr)); })() : (((head === "if-some") && (rest_items.length >= 2))) ? (() => { const binding = parse_cond_let_binding(rest_items[0]); return make_if_some(binding["name"], binding["expr"], parse_expr(rest_items[1]), ((rest_items.length >= 3) ? parse_expr(rest_items[2]) : null)); })() : ((head === "comment")) ? make_literal("nil", null) : ((head === "do")) ? make_do(rest_items.map(parse_expr)) : ((head === "cond")) ? make_cond(parse_cond_clauses(rest_items)) : (((head === "condp") && (rest_items.length >= 2))) ? parse_condp_form(rest_items[0], rest_items[1], rest_items.slice(2)) : ((head === "try")) ? parse_try_form(rest_items) : (((head === "match") && (rest_items.length >= 1))) ? parse_match_form(rest_items[0], rest_items.slice(1)) : (((head === "case") && (rest_items.length >= 1))) ? parse_case_form(rest_items[0], rest_items.slice(1)) : (((head === "doseq") && (rest_items.length >= 1))) ? make_doseq(parse_for_clauses(rest_items[0]), rest_items.slice(1).map(parse_expr)) : (((head === "dotimes") && (rest_items.length >= 1))) ? (() => { const binding_items = unwrap_items(rest_items[0]); return ((binding_items.length === 2) ? make_dotimes(binding_items[0], parse_expr(binding_items[1]), rest_items.slice(1).map(parse_expr)) : make_dotimes("_", make_literal("number", 0), [])); })() : (((head === "with") && (rest_items.length >= 1))) ? parse_with_form(rest_items[0], rest_items.slice(1)) : (((head === "fmt") && (rest_items.length === 1))) ? (() => { const arg = rest_items[0]; return (string_datum_p(arg) ? parse_expr(expand_fmt(extract_string(arg))) : parse_expr(arg)); })() : (((head === "->") && (rest_items.length >= 1))) ? parse_expr(expand_thread_first(rest_items[0], rest_items.slice(1))) : (((head === "->>") && (rest_items.length >= 1))) ? parse_expr(expand_thread_last(rest_items[0], rest_items.slice(1))) : (((head === "cond->") && (rest_items.length >= 1))) ? parse_expr(expand_cond_thread("cond->", rest_items[0], rest_items.slice(1))) : (((head === "cond->>") && (rest_items.length >= 1))) ? parse_expr(expand_cond_thread("cond->>", rest_items[0], rest_items.slice(1))) : (((head === "some->") && (rest_items.length >= 1))) ? parse_expr(expand_some_thread("some->", rest_items[0], rest_items.slice(1))) : (((head === "some->>") && (rest_items.length >= 1))) ? parse_expr(expand_some_thread("some->>", rest_items[0], rest_items.slice(1))) : (((head === "as->") && (rest_items.length >= 2))) ? parse_expr(expand_as_thread(rest_items[0], rest_items[1], rest_items.slice(2))) : (((typeof head === 'string') && constructor_sym_p(head))) ? make_new(head, rest_items.map(parse_expr)) : (((typeof head === 'string') && keyword_sym_p(head) && (rest_items.length >= 1))) ? make_kw_access(head, parse_expr(rest_items[0]), ((rest_items.length >= 2) ? parse_expr(rest_items[1]) : null)) : (((typeof head === 'string') && dot_method_sym_p(head) && (rest_items.length >= 1))) ? make_method_call(head, parse_expr(rest_items[0]), rest_items.slice(1).map(parse_expr)) : (((typeof head === 'string') && static_method_sym_p(head))) ? make_static_call(head, rest_items.map(parse_expr)) : (((typeof head === 'string'))) ? make_call(make_ref(head), rest_items.map(parse_expr)) : (Array.isArray(head)) ? make_call(parse_expr(head), rest_items.map(parse_expr)) : make_literal("nil", null); })();
}

function meta_form_p(d) {
  return (Array.isArray(d) && (d.length > 0) && META_FORMS.includes(d[0]));
}

function parse_program(datums) {
  return (() => { const mode = {["v"]: "strict"}; const namespace = {["v"]: "beagle.user"}; const target = {["v"]: "clj"}; const externs = {}; const requires = []; const forms = []; datums.forEach((d) => ((!Array.isArray(d)) ? null : ((d.length < 2) ? null : (() => { const head = d[0]; return ((head === "define-mode")) ? (mode["v"] = d[1]) : ((head === "define-target")) ? (target["v"] = d[1]) : ((head === "ns")) ? (namespace["v"] = d[1]) : (((head === "declare-extern") && (d.length >= 3))) ? (externs[d[1]] = ((d.length >= 3) ? parse_type(d[2]) : make_prim("Any"))) : ((head === "require")) ? (() => { const ns_name = d[1]; const has_as = ((d.length >= 4) && (d[2] === ":as")); const has_refer = ((d.length >= 4) && (d[2] === ":refer")); const alias = (has_as ? d[3] : null); const refer = (has_refer ? (() => { const bracket_form = d[3]; return ((Array.isArray(bracket_form) && (bracket_form[0] === "#%brackets")) ? bracket_form.slice(1) : null); })() : null); return requires.push({["ns"]: ns_name, ["alias"]: alias, ["refer"]: refer}); })() : null; })())));
datums.forEach((d) => (meta_form_p(d) ? null : forms.push(parse_expr(d))));
return (() => { const extern_keys = Object.keys(externs); const extern_list = extern_keys.map((k) => (() => { const t = externs[k]; return {["name"]: k, ["type"]: t}; })()); return {["mode"]: mode["v"], ["namespace"]: namespace["v"], ["target"]: target["v"], ["forms"]: forms, ["externs"]: extern_list, ["requires"]: requires}; })(); })();
}

