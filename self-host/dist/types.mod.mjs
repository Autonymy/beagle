
const PRIMITIVES = ["String", "Int", "Float", "Bool", "Keyword", "Symbol", "Nil", "Any"];

const CLJ_ALIASES = {["Long"]: "Int", ["Double"]: "Float", ["Boolean"]: "Bool", ["Integer"]: "Int"};

const PARAMETRIC_CTORS = ["Vec", "List", "Set", "Map", "Promise"];

function make_prim(name) {
  return {["kind"]: "prim", ["name"]: name};
}

function make_fn(params, rest_type, ret) {
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

function prim_p(t) {
  return ((!(t == null)) && (t["kind"] !== null) && (t["kind"] === "prim"));
}

function fn_type_p(t) {
  return ((!(t == null)) && (t["kind"] === "fn"));
}

function app_type_p(t) {
  return ((!(t == null)) && (t["kind"] === "app"));
}

function union_type_p(t) {
  return ((!(t == null)) && (t["kind"] === "union"));
}

function var_type_p(t) {
  return ((!(t == null)) && (t["kind"] === "var"));
}

function poly_type_p(t) {
  return ((!(t == null)) && (t["kind"] === "poly"));
}

function any_type_p(t) {
  return (prim_p(t) && (t["name"] === "Any"));
}

function type__gtstring(t) {
  return ((t == null)) ? "?" : (prim_p(t)) ? t["name"] : (fn_type_p(t)) ? (() => { const params = t["params"]; const rest_t = t["rest"]; const ret = t["ret"]; const param_strs = params.map((p) => type__gtstring(p)); return ((!(rest_t == null)) ? ("".concat("[", param_strs.join(" "), " & ", type__gtstring(rest_t), " -> ", type__gtstring(ret), "]")) : ("".concat("[", param_strs.join(" "), " -> ", type__gtstring(ret), "]"))); })() : (app_type_p(t)) ? (() => { const ctor = t["name"]; const args = t["args"]; const arg_strs = args.map((a) => type__gtstring(a)); return ("".concat("(", ctor, " ", arg_strs.join(" "), ")")); })() : (union_type_p(t)) ? (() => { const members = t["members"]; const all_prim = members.every((m) => prim_p(m)); const names = (all_prim ? members.map((m) => m["name"]) : []); return ((all_prim && (members.length === 2) && names.includes("Int") && names.includes("Float"))) ? "Number" : (((members.length === 2) && members.some((m) => (prim_p(m) && (m["name"] === "Nil"))))) ? (() => { const non_nil = members.find((m) => (!(prim_p(m) && (m["name"] === "Nil")))); return ("".concat(type__gtstring(non_nil), "?")); })() : ("".concat("(U ", members.map((m) => type__gtstring(m)).join(" "), ")")); })() : (var_type_p(t)) ? t["name"] : (poly_type_p(t)) ? (() => { const vars = t["vars"]; const body = t["body"]; const bounds = t["bounds"]; const var_strs = vars.map((v) => (() => { const b = (bounds ? bounds[v] : null); return (b ? ("".concat("(", v, " <: ", type__gtstring(b), ")")) : v); })()); return ("".concat("(forall [", var_strs.join(" "), "] ", type__gtstring(body), ")")); })() : "?";
}

function unqualify_name(name) {
  return (() => { const idx = name.indexOf("/"); return ((idx > -1) ? name.substring((idx + 1)) : name); })();
}

function type_compatible_p(actual, expected) {
  return (((actual == null) || (expected == null))) ? true : (any_type_p(actual)) ? true : (any_type_p(expected)) ? true : (var_type_p(actual)) ? true : (var_type_p(expected)) ? true : (poly_type_p(expected)) ? type_compatible_p(actual, expected["body"]) : (poly_type_p(actual)) ? type_compatible_p(actual["body"], expected) : ((union_type_p(actual) && union_type_p(expected))) ? actual["members"].every((a_alt) => expected["members"].some((e_alt) => type_compatible_p(a_alt, e_alt))) : (union_type_p(expected)) ? expected["members"].some((alt) => type_compatible_p(actual, alt)) : (union_type_p(actual)) ? actual["members"].every((alt) => type_compatible_p(alt, expected)) : ((prim_p(actual) && prim_p(expected))) ? ((actual["name"] === expected["name"]) || (unqualify_name(actual["name"]) === unqualify_name(expected["name"]))) : ((fn_type_p(actual) && fn_type_p(expected))) ? (() => { const ap = actual["params"]; const ep = expected["params"]; const ar = actual["rest"]; const er = expected["rest"]; return ((ap.length === ep.length) && ap.every((p, i) => type_compatible_p(p, ep[i])) && ((ar == null) === (er == null)) && ((ar == null) || type_compatible_p(ar, er)) && type_compatible_p(actual["ret"], expected["ret"])); })() : ((app_type_p(actual) && app_type_p(expected))) ? ((actual["name"] === expected["name"]) && (actual["args"].length === expected["args"].length) && actual["args"].every((a, i) => type_compatible_p(a, expected["args"][i]))) : false;
}

const user_parametric = {};

function parse_fn_type(items) {
  return (() => { const arrow_pos = items.indexOf("->"); return ((arrow_pos === -1) ? make_prim("Any") : (() => { const before = items.slice(0, arrow_pos); const after = items.slice((arrow_pos + 1)); return ((after.length !== 1) ? make_prim("Any") : (() => { const amp_pos = before.indexOf("&"); return ((amp_pos > -1) ? make_fn(before.slice(0, amp_pos).map(parse_type), parse_type(before.slice((amp_pos + 1))[0]), parse_type(after[0])) : make_fn(before.map(parse_type), null, parse_type(after[0]))); })()); })()); })();
}

function parse_type(t) {
  return ((Array.isArray(t) && (t.length > 0) && (t[0] === "#%brackets"))) ? parse_fn_type(t.slice(1)) : ((Array.isArray(t) && (t.length === 3) && (t[0] === "forall"))) ? (() => { const vars_form = t[1]; const raw_vars = ((Array.isArray(vars_form) && (vars_form.length > 0) && (vars_form[0] === "#%brackets")) ? vars_form.slice(1) : vars_form); const vars = raw_vars.filter((v) => (typeof v === 'string')); return make_poly(vars, parse_type(t[2]), null); })() : ((Array.isArray(t) && (t.length > 1) && (t[0] === "U"))) ? make_union(t.slice(1).map(parse_type)) : ((Array.isArray(t) && (t.length > 0) && (typeof t[0] === 'string') && (PARAMETRIC_CTORS.includes(t[0]) || user_parametric[t[0]]))) ? make_app(t[0], t.slice(1).map(parse_type)) : (((typeof t === 'string') && (t.length > 1) && (t.charAt((t.length - 1)) === "?"))) ? (() => { const base = t.substring(0, (t.length - 1)); return make_union([parse_type(base), make_prim("Nil")]); })() : (((typeof t === 'string') && (t === "Number"))) ? make_union([make_prim("Int"), make_prim("Float")]) : (((typeof t === 'string') && (!(CLJ_ALIASES[t] == null)))) ? make_prim(CLJ_ALIASES[t]) : ((typeof t === 'string')) ? make_prim(t) : make_prim("Any");
}

function infer_literal_type(e) {
  return (() => { const kind = e["kind"]; return ((kind === "string")) ? make_prim("String") : ((kind === "bool")) ? make_prim("Bool") : ((kind === "number")) ? make_prim("Int") : ((kind === "float")) ? make_prim("Float") : ((kind === "nil")) ? make_prim("Nil") : ((kind === "keyword")) ? make_prim("Keyword") : ((kind === "symbol")) ? make_prim("Symbol") : null; })();
}

function infer_type_var_bindings(expected, actual, bindings) {
  return (((expected == null) || (actual == null))) ? null : (any_type_p(actual)) ? null : (var_type_p(expected)) ? (() => { (() => { if ((bindings[expected["name"]] == null)) { return (bindings[expected["name"]] = actual); } })();
return null; })() : ((fn_type_p(expected) && fn_type_p(actual))) ? (() => { (() => { if ((expected["params"].length === actual["params"].length)) { return expected["params"].forEach((ep, i) => infer_type_var_bindings(ep, actual["params"][i], bindings)); } })();
(() => { if (((!(expected["rest"] == null)) && (!(actual["rest"] == null)))) { return infer_type_var_bindings(expected["rest"], actual["rest"], bindings); } })();
infer_type_var_bindings(expected["ret"], actual["ret"], bindings);
return null; })() : ((app_type_p(expected) && app_type_p(actual) && (expected["name"] === actual["name"]))) ? (() => { expected["args"].forEach((ea, i) => infer_type_var_bindings(ea, actual["args"][i], bindings));
return null; })() : null;
}

function apply_type_bindings(t, bindings) {
  return ((t == null)) ? null : (var_type_p(t)) ? (() => { const bound = bindings[t["name"]]; return ((bound == null) ? make_prim("Any") : bound); })() : (prim_p(t)) ? t : (fn_type_p(t)) ? make_fn(t["params"].map((p) => apply_type_bindings(p, bindings)), ((t["rest"] == null) ? null : apply_type_bindings(t["rest"], bindings)), apply_type_bindings(t["ret"], bindings)) : (app_type_p(t)) ? make_app(t["name"], t["args"].map((a) => apply_type_bindings(a, bindings))) : (union_type_p(t)) ? make_union(t["members"].map((m) => apply_type_bindings(m, bindings))) : (poly_type_p(t)) ? t : t;
}

