
const ANY = {["kind"]: "prim", ["name"]: "Any"};

const NIL_TYPE = {["kind"]: "prim", ["name"]: "Nil"};

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

function nil_type_p(t) {
  return (prim_p(t) && (t["name"] === "Nil"));
}

function type__gtstring(t) {
  return ((t == null)) ? "?" : (prim_p(t)) ? t["name"] : (fn_type_p(t)) ? (() => { const params = t["params"]; const rest_t = t["rest"]; const ret = t["ret"]; const param_strs = params.map((p) => type__gtstring(p)); return ((!(rest_t == null)) ? ("".concat("[", param_strs.join(" "), " & ", type__gtstring(rest_t), " -> ", type__gtstring(ret), "]")) : ("".concat("[", param_strs.join(" "), " -> ", type__gtstring(ret), "]"))); })() : (app_type_p(t)) ? (() => { const ctor = t["name"]; const args = t["args"]; const arg_strs = args.map((a) => type__gtstring(a)); return ("".concat("(", ctor, " ", arg_strs.join(" "), ")")); })() : (union_type_p(t)) ? (() => { const members = t["members"]; const member_strs = members.map((m) => type__gtstring(m)); return ("".concat("(U ", member_strs.join(" "), ")")); })() : (var_type_p(t)) ? t["name"] : (poly_type_p(t)) ? ("".concat("(forall [", t["vars"].join(" "), "] ", type__gtstring(t["body"]), ")")) : "?";
}

function unqualify_name(name) {
  return (() => { const idx = name.indexOf("/"); return ((idx > -1) ? name.substring((idx + 1)) : name); })();
}

function type_compatible_p(actual, expected) {
  return (((actual == null) || (expected == null))) ? true : (any_type_p(actual)) ? true : (any_type_p(expected)) ? true : (var_type_p(actual)) ? true : (var_type_p(expected)) ? true : (poly_type_p(expected)) ? type_compatible_p(actual, expected["body"]) : (poly_type_p(actual)) ? type_compatible_p(actual["body"], expected) : ((union_type_p(actual) && union_type_p(expected))) ? actual["members"].every((a_alt) => expected["members"].some((e_alt) => type_compatible_p(a_alt, e_alt))) : (union_type_p(expected)) ? expected["members"].some((alt) => type_compatible_p(actual, alt)) : (union_type_p(actual)) ? actual["members"].every((alt) => type_compatible_p(alt, expected)) : ((prim_p(actual) && prim_p(expected))) ? ((actual["name"] === expected["name"]) || (unqualify_name(actual["name"]) === unqualify_name(expected["name"]))) : ((fn_type_p(actual) && fn_type_p(expected))) ? (() => { const ap = actual["params"]; const ep = expected["params"]; const ar = actual["rest"]; const er = expected["rest"]; return ((ap.length === ep.length) && ap.every((p, i) => type_compatible_p(p, ep[i])) && ((ar == null) === (er == null)) && ((ar == null) || type_compatible_p(ar, er)) && type_compatible_p(actual["ret"], expected["ret"])); })() : ((app_type_p(actual) && app_type_p(expected))) ? ((actual["name"] === expected["name"]) && (actual["args"].length === expected["args"].length) && actual["args"].every((a, i) => type_compatible_p(a, expected["args"][i]))) : false;
}

function type_equal_p(a, b) {
  return (prim_p(a) && prim_p(b) && (a["name"] === b["name"]));
}

function merge_types(t1, t2) {
  return ((any_type_p(t1) && any_type_p(t2))) ? ANY : (any_type_p(t1)) ? t2 : (any_type_p(t2)) ? t1 : (type_compatible_p(t1, t2)) ? t1 : (() => { const flat1 = (union_type_p(t1) ? t1["members"] : [t1]); const flat2 = (union_type_p(t2) ? t2["members"] : [t2]); const all = flat1.concat(flat2); const deduped = all.reduce((acc, t) => (acc.some((a) => type_compatible_p(t, a)) ? acc : acc.concat([t])), []); return ((deduped.length === 1) ? deduped[0] : make_union(deduped)); })();
}

function merge_types_list(types) {
  return ((types.length === 0) ? ANY : types.reduce((acc, t) => merge_types(acc, t), types[0]));
}

function remove_from_union(current_type, remove_type) {
  return (any_type_p(current_type)) ? current_type : (union_type_p(current_type)) ? (() => { const alts = current_type["members"]; const remaining = alts.filter((alt) => (!type_equal_p(alt, remove_type))); return ((remaining.length === alts.length)) ? current_type : ((remaining.length === 0)) ? current_type : ((remaining.length === 1)) ? remaining[0] : make_union(remaining); })() : current_type;
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

const STATE = {["record-fields"]: {}, ["record-field-order"]: {}, ["union-members"]: {}, ["parametric-unions"]: {}, ["diagnostics"]: []};

function emit_diag(msg) {
  STATE["diagnostics"].push(msg);
  process.stderr.write(("".concat(msg, "\n")));
  return null;
}

function copy_env(env) {
  return Object.assign({}, env);
}

function param_type_or_any(p) {
  return (() => { const t = p["type"]; return ((t === "param")) ? ((p["ann"] == null) ? ANY : p["ann"]) : ((t === "map-destructure")) ? ANY : ((t === "seq-destructure")) ? ANY : ANY; })();
}

function extend_with_params(env, params, rest_param) {
  return (() => { const out = copy_env(env); params.forEach((p) => (() => { const t = p["type"]; return ((t === "param")) ? (() => { (out[p["name"]] = param_type_or_any(p));
return null; })() : ((t === "map-destructure")) ? (() => { p["keys"].forEach((k) => { (out[k] = ANY);
return null; });
(() => { if ((!(p["as"] == null))) { return (out[p["as"]] = ANY); } })();
return null; })() : ((t === "seq-destructure")) ? (() => { p["names"].forEach((n) => { (out[n] = ANY);
return null; });
(() => { if ((!(p["rest"] == null))) { return (out[p["rest"]] = ANY); } })();
return null; })() : null; })());
(() => { if ((!(rest_param == null))) { return (() => { if ((rest_param["type"] === "param")) { return (out[rest_param["name"]] = param_type_or_any(rest_param)); } })(); } })();
return out; })();
}

function extend_with_let_bindings(env, bindings) {
  return (() => { const out = copy_env(env); bindings.forEach((b) => (() => { const inferred = infer_expr(b["value"], out); const declared = b["ann"]; const bname = b["name"]; (() => { if (((!(declared == null)) && (!type_compatible_p(inferred, declared)))) { return emit_diag(("".concat("beagle: let binding ", bname, ": expected ", type__gtstring(declared), ", got ", type__gtstring(inferred)))); } })();
(out[bname] = ((!(declared == null)) ? declared : inferred));
return null; })());
return out; })();
}

const TYPE_PREDICATES = {["nil?"]: "Nil", ["string?"]: "String", ["number?"]: "Int", ["integer?"]: "Int", ["keyword?"]: "Keyword", ["symbol?"]: "Symbol", ["boolean?"]: "Bool", ["float?"]: "Float", ["int?"]: "Int"};

function extract_narrowing(cond_expr) {
  return ((cond_expr["node"] !== "call")) ? {["var"]: null, ["type"]: null, ["negated"]: false} : (() => { const fn_ref = cond_expr["fn"]; const fn_name = (((!(fn_ref == null)) && (fn_ref["node"] === "ref")) ? fn_ref["name"] : ((typeof fn_ref === 'string') ? fn_ref : null)); const args = cond_expr["args"]; return (((!(fn_name == null)) && (!(TYPE_PREDICATES[fn_name] == null)) && (args.length === 1) && (args[0]["node"] === "ref"))) ? {["var"]: args[0]["name"], ["type"]: make_prim(TYPE_PREDICATES[fn_name]), ["negated"]: false} : (((!(fn_name == null)) && (fn_name === "some?") && (args.length === 1) && (args[0]["node"] === "ref"))) ? {["var"]: args[0]["name"], ["type"]: make_prim("Nil"), ["negated"]: true} : (((!(fn_name == null)) && (fn_name === "=") && (args.length === 2))) ? (() => { const a1 = args[0]; const a2 = args[1]; return (((a1["node"] === "ref") && (a2["node"] === "literal") && (a2["kind"] === "nil"))) ? {["var"]: a1["name"], ["type"]: make_prim("Nil"), ["negated"]: false} : (((a1["node"] === "literal") && (a1["kind"] === "nil") && (a2["node"] === "ref"))) ? {["var"]: a2["name"], ["type"]: make_prim("Nil"), ["negated"]: false} : {["var"]: null, ["type"]: null, ["negated"]: false}; })() : (((!(fn_name == null)) && (fn_name === "not") && (args.length === 1))) ? (() => { const inner = extract_narrowing(args[0]); return ((!(inner["var"] == null)) ? {["var"]: inner["var"], ["type"]: inner["type"], ["negated"]: (!inner["negated"])} : {["var"]: null, ["type"]: null, ["negated"]: false}); })() : {["var"]: null, ["type"]: null, ["negated"]: false}; })();
}

function narrow_env_for_condition(env, cond_expr) {
  return (() => { const info = extract_narrowing(cond_expr); const v = info["var"]; const narrow_type = info["type"]; const negated = info["negated"]; return ((v == null) ? {["then"]: env, ["else"]: env} : (() => { const current_type = env[v]; return ((current_type == null) ? {["then"]: env, ["else"]: env} : (() => { const pos_env = copy_env(env); const neg_env = copy_env(env); (pos_env[v] = narrow_type);
(neg_env[v] = remove_from_union(current_type, narrow_type));
return (negated ? {["then"]: neg_env, ["else"]: pos_env} : {["then"]: pos_env, ["else"]: neg_env}); })()); })()); })();
}

function narrow_env_for_match(clause, target_type, env) {
  return (() => { const pat = clause["pattern"]; return ((pat["type"] === "record")) ? (() => { const rec_name = pat["name"]; const bindings = pat["bindings"]; const arm_env = copy_env(env); const field_map = STATE["record-fields"][rec_name]; const field_order = STATE["record-field-order"][rec_name]; return ((!(field_map == null)) ? (() => { bindings.forEach((b, i) => (() => { const kw = ((!(field_order == null)) ? ((i < field_order.length) ? field_order[i] : null) : null); const raw_type = (((!(kw == null)) && (!(field_map[kw] == null))) ? field_map[kw] : ANY); const bname = ((typeof b === 'string') ? b : b["name"]); (arm_env[bname] = raw_type);
return null; })());
return arm_env; })() : (() => { (() => { if ((bindings.length === 1)) { return (() => { const bname = ((typeof bindings[0] === 'string') ? bindings[0] : bindings[0]["name"]); return (arm_env[bname] = make_prim(rec_name)); })(); } })();
return arm_env; })()); })() : ((pat["type"] === "var")) ? (() => { const arm_env = copy_env(env); (arm_env[pat["name"]] = target_type);
return arm_env; })() : env; })();
}

function check_match_exhaustiveness(target_type, clauses) {
  return (() => { const union_name = (prim_p(target_type)) ? target_type["name"] : (app_type_p(target_type)) ? target_type["name"] : null; const union_members = ((!(union_name == null)) ? STATE["union-members"][union_name] : null); (() => { if ((!(union_members == null))) { return (() => { const matched_types = clauses.reduce((acc, c) => (() => { const pat = c["pattern"]; return ((pat["type"] === "record") ? acc.concat([pat["name"]]) : acc); })(), []); const missing = union_members.filter((m) => (matched_types.indexOf(m) === -1)); return (() => { if ((missing.length > 0)) { return emit_diag(("".concat("beagle: match on ", union_name, " is not exhaustive; missing cases: ", missing.join(", ")))); } })(); })(); } })();
return null; })();
}

function lookup_kw_field_type(kw, target_type) {
  return ((prim_p(target_type) && (!(STATE["record-fields"][target_type["name"]] == null)))) ? (() => { const field_map = STATE["record-fields"][target_type["name"]]; const result = field_map[kw]; return ((result == null) ? ANY : result); })() : ANY;
}

function resolve_poly_call(poly_t, args, env) {
  return (() => { const body = poly_t["body"]; const bindings = {}; const arg_types = args.map((a) => infer_expr(a, env)); const fixed = body["params"]; const rest_t = body["rest"]; const n_fixed = fixed.length; fixed.forEach((pt, i) => { (() => { if ((i < arg_types.length)) { return infer_type_var_bindings(pt, arg_types[i], bindings); } })();
return null; });
(() => { if (((!(rest_t == null)) && (arg_types.length > n_fixed))) { return arg_types.slice(n_fixed).forEach((at) => { infer_type_var_bindings(rest_t, at, bindings);
return null; }); } })();
return apply_type_bindings(body, bindings); })();
}

function last_expr_type(body, env) {
  return ((body.length === 0) ? ANY : body.reduce((acc, e) => infer_expr(e, env), ANY));
}

function check_args(fn_name, fn_t, args, env) {
  return (() => { const fixed = fn_t["params"]; const rest_t = fn_t["rest"]; const n_fixed = fixed.length; const n_args = args.length; const sig_str = ("".concat(fn_name, " : ", type__gtstring(fn_t))); return ((!(rest_t == null))) ? (() => { (() => { if ((n_args < n_fixed)) { return emit_diag(("".concat("beagle: call to ", fn_name, ": expected at least ", ("".concat(n_fixed)), " arg(s), got ", ("".concat(n_args))))); } })();
args.slice(0, n_fixed).forEach((a, i) => { (() => { const expected = fixed[i]; const actual = infer_expr(a, env); return (() => { if ((!type_compatible_p(actual, expected))) { return emit_diag(("".concat("beagle: call to ", fn_name, ": arg ", ("".concat((i + 1))), " expected ", type__gtstring(expected), ", got ", type__gtstring(actual)))); } })(); })();
return null; });
args.slice(n_fixed).forEach((a, idx) => { (() => { const actual = infer_expr(a, env); return (() => { if ((!type_compatible_p(actual, rest_t))) { return emit_diag(("".concat("beagle: call to ", fn_name, ": rest arg ", ("".concat((n_fixed + idx + 1))), " expected ", type__gtstring(rest_t), ", got ", type__gtstring(actual)))); } })(); })();
return null; });
return null; })() : (() => { (() => { if ((n_fixed !== n_args)) { return emit_diag(("".concat("beagle: call to ", fn_name, ": expected ", ("".concat(n_fixed)), " arg(s), got ", ("".concat(n_args))))); } })();
args.slice(0, ((n_fixed < n_args) ? n_fixed : n_args)).forEach((a, i) => { (() => { if ((i < n_fixed)) { return (() => { const expected = fixed[i]; const actual = infer_expr(a, env); return (() => { if ((!type_compatible_p(actual, expected))) { return emit_diag(("".concat("beagle: call to ", fn_name, ": arg ", ("".concat((i + 1))), " expected ", type__gtstring(expected), ", got ", type__gtstring(actual)))); } })(); })(); } })();
return null; });
return null; })(); })();
}

function infer_cond_clauses(clauses, env) {
  return (() => { const state = {["result"]: ANY, ["env"]: env}; clauses.forEach((c) => { (() => { const test = c["test"]; const cur_env = state["env"]; infer_expr(test, cur_env);
return (() => { const narrowed = narrow_env_for_condition(cur_env, test); const then_env = narrowed["then"]; const else_env = narrowed["else"]; const body_type = last_expr_type(c["body"], then_env); (state["result"] = merge_types(state["result"], body_type));
return (state["env"] = else_env); })(); })();
return null; });
return state["result"]; })();
}

function infer_expr(e, env) {
  return ((e == null)) ? ANY : ((e["node"] === "literal")) ? (() => { const t = infer_literal_type(e); return ((t == null) ? ANY : t); })() : ((e["node"] === "ref")) ? (() => { const name = e["name"]; const found = env[name]; return ((found == null) ? ANY : found); })() : ((e["node"] === "def")) ? (() => { const inferred = infer_expr(e["value"], env); const expected = e["ann"]; (() => { if (((!(expected == null)) && (!type_compatible_p(inferred, expected)))) { return emit_diag(("".concat("beagle: def ", e["name"], ": expected ", type__gtstring(expected), ", got ", type__gtstring(inferred)))); } })();
return inferred; })() : ((e["node"] === "defonce")) ? (() => { const inferred = infer_expr(e["value"], env); const expected = e["ann"]; (() => { if (((!(expected == null)) && (!type_compatible_p(inferred, expected)))) { return emit_diag(("".concat("beagle: defonce ", e["name"], ": expected ", type__gtstring(expected), ", got ", type__gtstring(inferred)))); } })();
return inferred; })() : ((e["node"] === "defn")) ? (() => { const params = e["params"]; const rest_param = e["rest-param"]; const expected_ret = e["ret"]; const body_env = extend_with_params(env, params, rest_param); const body_type = last_expr_type(e["body"], body_env); (() => { if (((!(expected_ret == null)) && (!type_compatible_p(body_type, expected_ret)))) { return (() => { const is_promise = (app_type_p(expected_ret) && (expected_ret["name"] === "Promise") && (expected_ret["args"].length === 1) && type_compatible_p(body_type, expected_ret["args"][0])); return (() => { if ((!is_promise)) { return emit_diag(("".concat("beagle: defn ", e["name"], ": expected return ", type__gtstring(expected_ret), ", got ", type__gtstring(body_type)))); } })(); })(); } })();
return ANY; })() : ((e["node"] === "defn-multi")) ? (() => { e["arities"].forEach((a) => { (() => { const body_env = extend_with_params(env, a["params"], a["rest"]); const body_type = last_expr_type(a["body"], body_env); const expected_ret = a["ret"]; return (() => { if (((!(expected_ret == null)) && (!type_compatible_p(body_type, expected_ret)))) { return emit_diag(("".concat("beagle: defn ", e["name"], " (", ("".concat(a["params"].length)), "-arity): expected return ", type__gtstring(expected_ret), ", got ", type__gtstring(body_type)))); } })(); })();
return null; });
return ANY; })() : ((e["node"] === "fn")) ? (() => { const params = e["params"]; const p_types = params.map(param_type_or_any); const body_env = extend_with_params(env, params, e["rest-param"]); const ret = ((!(e["ret"] == null)) ? e["ret"] : last_expr_type(e["body"], body_env)); return make_fn(p_types, null, ret); })() : ((e["node"] === "let")) ? (() => { const body_env = extend_with_let_bindings(env, e["bindings"]); return last_expr_type(e["body"], body_env); })() : ((e["node"] === "letfn")) ? (() => { const body_env = copy_env(env); e["fns"].forEach((f) => { (() => { const p_types = f["params"].map(param_type_or_any); const rtype = ((!(f["rest"] == null)) ? param_type_or_any(f["rest"]) : null); const ret = ((!(f["ret"] == null)) ? f["ret"] : ANY); return (body_env[f["name"]] = make_fn(p_types, rtype, ret)); })();
return null; });
e["fns"].forEach((f) => { (() => { const fn_env = extend_with_params(body_env, f["params"], f["rest"]); return last_expr_type(f["body"], fn_env); })();
return null; });
return last_expr_type(e["body"], body_env); })() : ((e["node"] === "if")) ? (() => { infer_expr(e["cond"], env);
return (() => { const narrowed = narrow_env_for_condition(env, e["cond"]); const then_env = narrowed["then"]; const else_env = narrowed["else"]; const tt = infer_expr(e["then"], then_env); const et = ((!(e["else"] == null)) ? infer_expr(e["else"], else_env) : NIL_TYPE); return merge_types(tt, et); })(); })() : ((e["node"] === "when")) ? (() => { infer_expr(e["cond"], env);
return (() => { const narrowed = narrow_env_for_condition(env, e["cond"]); const then_env = narrowed["then"]; return last_expr_type(e["body"], then_env); })(); })() : ((e["node"] === "do")) ? last_expr_type(e["body"], env) : ((e["node"] === "cond")) ? (() => { const clauses = e["clauses"]; return ((clauses.length === 0) ? ANY : infer_cond_clauses(clauses, env)); })() : ((e["node"] === "loop")) ? (() => { const body_env = extend_with_let_bindings(env, e["bindings"]); return last_expr_type(e["body"], body_env); })() : ((e["node"] === "recur")) ? (() => { e["args"].forEach((a) => { infer_expr(a, env);
return null; });
return ANY; })() : ((e["node"] === "set!")) ? (() => { infer_expr(e["target"], env);
infer_expr(e["value"], env);
return ANY; })() : ((e["node"] === "await")) ? (() => { const inner_type = infer_expr(e["expr"], env); return ((app_type_p(inner_type) && (inner_type["name"] === "Promise") && (inner_type["args"].length === 1)) ? inner_type["args"][0] : ANY); })() : ((e["node"] === "vec")) ? (() => { const items = e["items"]; return ((items.length === 0) ? make_app("Vec", [ANY]) : (() => { const elem_types = items.map((it) => infer_expr(it, env)); const first_t = elem_types[0]; const all_same = ((!any_type_p(first_t)) && elem_types.slice(1).every((t) => type_compatible_p(t, first_t))); return (all_same ? make_app("Vec", [first_t]) : make_app("Vec", [ANY])); })()); })() : ((e["node"] === "map")) ? (() => { const pairs = e["pairs"]; return ((pairs.length === 0) ? make_app("Map", [ANY, ANY]) : (() => { const key_types = pairs.map((p) => infer_expr(p["key"], env)); const val_types = pairs.map((p) => infer_expr(p["val"], env)); const first_k = key_types[0]; const first_v = val_types[0]; const kt = (((!any_type_p(first_k)) && key_types.slice(1).every((t) => type_compatible_p(t, first_k))) ? first_k : ANY); const vt = (((!any_type_p(first_v)) && val_types.slice(1).every((t) => type_compatible_p(t, first_v))) ? first_v : ANY); return make_app("Map", [kt, vt]); })()); })() : ((e["node"] === "set")) ? (() => { const items = e["items"]; return ((items.length === 0) ? make_app("Set", [ANY]) : (() => { const elem_types = items.map((it) => infer_expr(it, env)); const first_t = elem_types[0]; const all_same = ((!any_type_p(first_t)) && elem_types.slice(1).every((t) => type_compatible_p(t, first_t))); return (all_same ? make_app("Set", [first_t]) : make_app("Set", [ANY])); })()); })() : ((e["node"] === "quoted")) ? ANY : ((e["node"] === "regex")) ? ANY : (((e["node"] === "unsafe") || (e["node"] === "unsafe-raw"))) ? ANY : ((e["node"] === "when-let")) ? (() => { const val_type = infer_expr(e["expr"], env); const body_env = copy_env(env); (body_env[e["name"]] = val_type);
last_expr_type(e["body"], body_env);
return NIL_TYPE; })() : ((e["node"] === "if-let")) ? (() => { const val_type = infer_expr(e["expr"], env); const then_env = copy_env(env); (then_env[e["name"]] = val_type);
return (() => { const then_type = infer_expr(e["then"], then_env); const else_type = ((!(e["else"] == null)) ? infer_expr(e["else"], env) : NIL_TYPE); return merge_types(then_type, else_type); })(); })() : ((e["node"] === "when-some")) ? (() => { const val_type = infer_expr(e["expr"], env); const body_env = copy_env(env); (body_env[e["name"]] = val_type);
last_expr_type(e["body"], body_env);
return NIL_TYPE; })() : ((e["node"] === "if-some")) ? (() => { const val_type = infer_expr(e["expr"], env); const then_env = copy_env(env); (then_env[e["name"]] = val_type);
return (() => { const then_type = infer_expr(e["then"], then_env); const else_type = infer_expr(e["else"], env); return merge_types(then_type, else_type); })(); })() : ((e["node"] === "condp")) ? (() => { infer_expr(e["pred"], env);
infer_expr(e["test"], env);
return (() => { const clause_types = e["clauses"].map((c) => { infer_expr(c["test"], env);
return infer_expr(c["body"], env); }); return ((!(e["default"] == null)) ? merge_types_list(clause_types.concat([infer_expr(e["default"], env)])) : ((clause_types.length === 0) ? ANY : merge_types_list(clause_types))); })(); })() : ((e["node"] === "dotimes")) ? (() => { infer_expr(e["count"], env);
return (() => { const body_env = copy_env(env); (body_env[e["name"]] = make_prim("Int"));
last_expr_type(e["body"], body_env);
return NIL_TYPE; })(); })() : ((e["node"] === "for")) ? (() => { const body_env = copy_env(env); e["clauses"].forEach((c) => { (() => { const ct = c["type"]; return ((ct === "binding")) ? (() => { const coll_type = infer_expr(c["expr"], body_env); const elem_type = ((app_type_p(coll_type) && ((coll_type["name"] === "Vec") || (coll_type["name"] === "List") || (coll_type["name"] === "Set")) && (coll_type["args"].length === 1)) ? coll_type["args"][0] : ANY); (body_env[c["name"]] = elem_type);
return null; })() : ((ct === "when")) ? (() => { infer_expr(c["test"], body_env);
return null; })() : ((ct === "let")) ? (() => { c["bindings"].forEach((b) => (() => { const t = infer_expr(b["value"], body_env); (body_env[b["name"]] = t);
return null; })());
return null; })() : null; })();
return null; });
return (() => { const body_type = last_expr_type(e["body"], body_env); return (any_type_p(body_type) ? make_app("Vec", [ANY]) : make_app("Vec", [body_type])); })(); })() : ((e["node"] === "doseq")) ? (() => { const body_env = copy_env(env); e["clauses"].forEach((c) => { (() => { const ct = c["type"]; return ((ct === "binding")) ? (() => { const coll_type = infer_expr(c["expr"], body_env); const elem_type = ((app_type_p(coll_type) && ((coll_type["name"] === "Vec") || (coll_type["name"] === "List") || (coll_type["name"] === "Set")) && (coll_type["args"].length === 1)) ? coll_type["args"][0] : ANY); (body_env[c["name"]] = elem_type);
return null; })() : ((ct === "when")) ? (() => { infer_expr(c["test"], body_env);
return null; })() : null; })();
return null; });
last_expr_type(e["body"], body_env);
return ANY; })() : ((e["node"] === "match")) ? (() => { const target_type = infer_expr(e["target"], env); const clauses = e["clauses"]; const arm_types = clauses.map((c) => (() => { const arm_env = narrow_env_for_match(c, target_type, env); return last_expr_type(c["body"], arm_env); })()); check_match_exhaustiveness(target_type, clauses);
return ((arm_types.length === 0) ? ANY : merge_types_list(arm_types)); })() : ((e["node"] === "case")) ? (() => { infer_expr(e["test"], env);
return (() => { const clause_types = e["clauses"].map((c) => infer_expr(c["body"], env)); const fallback_type = ((!(e["default"] == null)) ? infer_expr(e["default"], env) : NIL_TYPE); return merge_types_list([fallback_type].concat(clause_types)); })(); })() : ((e["node"] === "try")) ? (() => { const body_type = last_expr_type(e["body"], env); const catch_types = e["catches"].map((c) => (() => { const catch_env = copy_env(env); (() => { if ((!(c["name"] == null))) { return (catch_env[c["name"]] = ANY); } })();
return last_expr_type(c["body"], catch_env); })()); (() => { if ((!(e["finally"] == null))) { return last_expr_type(e["finally"], env); } })();
return merge_types_list([body_type].concat(catch_types)); })() : ((e["node"] === "new")) ? (() => { e["args"].forEach((a) => { infer_expr(a, env);
return null; });
return ANY; })() : ((e["node"] === "kw-access")) ? (() => { return (() => { const target_type = infer_expr(e["target"], env); (() => { if ((!(e["default"] == null))) { return infer_expr(e["default"], env); } })();
return lookup_kw_field_type(e["kw"], target_type); })(); })() : ((e["node"] === "with")) ? (() => { const target_type = infer_expr(e["target"], env); return ((prim_p(target_type) && (!(STATE["record-fields"][target_type["name"]] == null)))) ? (() => { const rec_name = target_type["name"]; const field_map = STATE["record-fields"][rec_name]; e["updates"].forEach((u) => { (() => { const kw = u["field"]; const val_type = infer_expr(u["value"], env); const expected = field_map[kw]; return ((!(expected == null))) ? (() => { if ((!type_compatible_p(val_type, expected))) { return emit_diag(("".concat("beagle: with ", rec_name, ": field ", kw, " expected ", type__gtstring(expected), ", got ", type__gtstring(val_type)))); } })() : emit_diag(("".concat("beagle: with ", rec_name, ": no field ", kw))); })();
return null; });
return target_type; })() : (() => { e["updates"].forEach((u) => { infer_expr(u["value"], env);
return null; });
return ANY; })(); })() : ((e["node"] === "record")) ? make_prim(e["name"]) : ((e["node"] === "defrecord")) ? (() => { register_record(e["name"], e["fields"], env);
return ANY; })() : ((e["node"] === "defunion")) ? (() => { register_union(e["name"], e["members"], e["type-params"], e["member-fields"], env);
return ANY; })() : ((e["node"] === "defenum")) ? ANY : ((e["node"] === "block-string")) ? make_prim("String") : ((e["node"] === "method-call")) ? (() => { const method_name = e["method"]; const target_type = infer_expr(e["target"], env); const all_args = [e["target"]].concat(e["args"]); const raw_type = env[method_name]; return (((!(raw_type == null)) && poly_type_p(raw_type))) ? (() => { const resolved = resolve_poly_call(raw_type, all_args, env); return (fn_type_p(resolved) ? (() => { check_args(method_name, resolved, all_args, env);
return resolved["ret"]; })() : (() => { e["args"].forEach((a) => { infer_expr(a, env);
return null; });
return ANY; })()); })() : (((!(raw_type == null)) && fn_type_p(raw_type))) ? (() => { check_args(method_name, raw_type, all_args, env);
return raw_type["ret"]; })() : (() => { e["args"].forEach((a) => { infer_expr(a, env);
return null; });
return ANY; })(); })() : ((e["node"] === "static-call")) ? (() => { const sym = e["name"]; const raw_type = env[sym]; return (((!(raw_type == null)) && poly_type_p(raw_type))) ? (() => { const resolved = resolve_poly_call(raw_type, e["args"], env); return (fn_type_p(resolved) ? (() => { check_args(sym, resolved, e["args"], env);
return resolved["ret"]; })() : (() => { e["args"].forEach((a) => { infer_expr(a, env);
return null; });
return ANY; })()); })() : (((!(raw_type == null)) && fn_type_p(raw_type))) ? (() => { check_args(sym, raw_type, e["args"], env);
return raw_type["ret"]; })() : (() => { e["args"].forEach((a) => { infer_expr(a, env);
return null; });
return ANY; })(); })() : ((e["node"] === "call")) ? (() => { const fn_ref = e["fn"]; const fn_name = (((!(fn_ref == null)) && (fn_ref["node"] === "ref"))) ? fn_ref["name"] : ((typeof fn_ref === 'string')) ? fn_ref : null; const args = e["args"]; return ((fn_name == null) ? (() => { (() => { if ((!(fn_ref == null))) { return infer_expr(fn_ref, env); } })();
args.forEach((a) => { infer_expr(a, env);
return null; });
return ANY; })() : (() => { const raw_type = env[fn_name]; return (((!(raw_type == null)) && poly_type_p(raw_type))) ? (() => { const resolved = resolve_poly_call(raw_type, args, env); return (fn_type_p(resolved) ? (() => { check_args(fn_name, resolved, args, env);
return resolved["ret"]; })() : (() => { args.forEach((a) => { infer_expr(a, env);
return null; });
return ANY; })()); })() : (((!(raw_type == null)) && fn_type_p(raw_type))) ? (() => { check_args(fn_name, raw_type, args, env);
return raw_type["ret"]; })() : (((!(raw_type == null)) && union_type_p(raw_type) && raw_type["members"].every((m) => fn_type_p(m)))) ? (() => { const n_args = args.length; const matching = raw_type["members"].find((alt) => (alt["params"].length === n_args)); return ((!(matching == null)) ? (() => { check_args(fn_name, matching, args, env);
return matching["ret"]; })() : (() => { emit_diag(("".concat("beagle: call to ", fn_name, ": no arity accepts ", ("".concat(n_args)), " arg(s)")));
return ANY; })()); })() : (() => { args.forEach((a) => { infer_expr(a, env);
return null; });
return ANY; })(); })()); })() : ((e["node"] === "dynamic-var")) ? (() => { const found = env[e["name"]]; return ((found == null) ? ANY : found); })() : ((e["node"] === "target-case")) ? (() => { e["cases"].forEach((c) => { infer_expr(c["body"], env);
return null; });
return ANY; })() : ((e["node"] === "defscalar")) ? ANY : ((e["node"] === "deferror")) ? ANY : ((e["node"] === "with-meta")) ? infer_expr(e["expr"], env) : (((e["node"] === "protocol") || (e["node"] === "deftype") || (e["node"] === "extend-type"))) ? ANY : (((e["node"] === "defmulti") || (e["node"] === "defmethod"))) ? ANY : ((e["node"] === "check")) ? (() => { const inner_type = infer_expr(e["expr"], env); return ((app_type_p(inner_type) && (inner_type["args"].length >= 1)) ? inner_type["args"][0] : ANY); })() : ((e["node"] === "rescue")) ? (() => { const inner_type = infer_expr(e["expr"], env); const fallback_env = ((!(e["err"] == null)) ? (() => { const e2 = copy_env(env); (e2[e["err"]] = ANY);
return e2; })() : env); const fallback_type = infer_expr(e["fallback"], fallback_env); return ((app_type_p(inner_type) && (inner_type["args"].length >= 1)) ? inner_type["args"][0] : fallback_type); })() : ((e["node"] === "ns")) ? ANY : (((e["node"] === "nix-inherit") || (e["node"] === "nix-inherit-from") || (e["node"] === "nix-with") || (e["node"] === "nix-rec-attrs") || (e["node"] === "nix-assert") || (e["node"] === "nix-get-or") || (e["node"] === "nix-has-attr") || (e["node"] === "nix-search-path") || (e["node"] === "nix-interpolated-string") || (e["node"] === "nix-multiline-string") || (e["node"] === "nix-indented-string") || (e["node"] === "nix-path") || (e["node"] === "nix-fn-set") || (e["node"] === "nix-pipe") || (e["node"] === "nix-impl"))) ? ANY : ANY;
}

function register_record(name, fields, env) {
  return (() => { const rec_type = make_prim(name); const name_lower = name.toLowerCase(); const field_map = {}; const field_order = []; fields.forEach((f) => { (() => { const fname = f["name"]; const ftype = ((f["ann"] == null) ? ANY : f["ann"]); const kw = ("".concat(":", fname)); (field_map[kw] = ftype);
field_order.push(kw);
return (env[("".concat(name_lower, "-", fname))] = make_fn([rec_type], null, ftype)); })();
return null; });
(env[("".concat("->", name))] = make_fn(fields.map((f) => ((f["ann"] == null) ? ANY : f["ann"])), null, rec_type));
(STATE["record-fields"][name] = field_map);
(STATE["record-field-order"][name] = field_order);
return null; })();
}

function register_union(name, members, type_params, member_fields, env) {
  (STATE["union-members"][name] = members);
  return (((type_params == null) || (type_params.length === 0)) ? (() => { (env[name] = make_union(members.map((m) => make_prim(m))));
(() => { if ((!(member_fields == null))) { return members.forEach((m) => { (() => { const fields = member_fields[m]; return (() => { if ((!(fields == null))) { return register_record(m, fields, env); } })(); })();
return null; }); } })();
return null; })() : (() => { (env[name] = make_prim(name));
(STATE["parametric-unions"][name] = {["params"]: type_params, ["members"]: members, ["member-fields"]: member_fields});
(() => { if ((!(member_fields == null))) { return members.forEach((m) => { (() => { const fields = member_fields[m]; return (() => { if ((!(fields == null))) { return (() => { const m_type = make_prim(m); const m_lower = m.toLowerCase(); const field_map = {}; const field_order = []; const ctor_fn = make_fn(fields.map((f) => ((f["ann"] == null) ? ANY : f["ann"])), null, m_type); (env[("".concat("->", m))] = make_poly(type_params, ctor_fn, null));
fields.forEach((f) => { (() => { const fname = f["name"]; const ftype = ((f["ann"] == null) ? ANY : f["ann"]); const kw = ("".concat(":", fname)); const acc_fn = make_fn([m_type], null, ftype); (field_map[kw] = ftype);
field_order.push(kw);
return (env[("".concat(m_lower, "-", fname))] = make_poly(type_params, acc_fn, null)); })();
return null; });
(STATE["record-fields"][m] = field_map);
return (STATE["record-field-order"][m] = field_order); })(); } })(); })();
return null; }); } })();
return null; })());
}

function build_initial_env(prog) {
  return (() => { const env = {}; const externs = prog["externs"]; const forms = prog["forms"]; (() => { if ((!(externs == null))) { return externs.forEach((ext) => { (env[ext["name"]] = ext["type"]);
return null; }); } })();
forms.forEach((raw_form) => { (() => { const form = ((raw_form["node"] === "with-meta") ? raw_form["expr"] : raw_form); const node = form["node"]; return ((node === "def")) ? (() => { if ((!(form["ann"] == null))) { return (env[form["name"]] = form["ann"]); } })() : ((node === "defonce")) ? (() => { if ((!(form["ann"] == null))) { return (env[form["name"]] = form["ann"]); } })() : ((node === "defn")) ? (() => { const params = form["params"]; const rest_param = form["rest-param"]; const p_types = params.map(param_type_or_any); const rtype = ((!(rest_param == null)) ? param_type_or_any(rest_param) : null); const ret = ((!(form["ret"] == null)) ? form["ret"] : ANY); return (env[form["name"]] = make_fn(p_types, rtype, ret)); })() : ((node === "defn-multi")) ? (() => { const arities = form["arities"]; const alt_types = arities.map((a) => (() => { const rp = a["rest"]; const p_types = a["params"].map(param_type_or_any); const rtype = ((!(rp == null)) ? param_type_or_any(rp) : null); const ret = ((!(a["ret"] == null)) ? a["ret"] : ANY); return make_fn(p_types, rtype, ret); })()); return (env[form["name"]] = ((alt_types.length === 1) ? alt_types[0] : make_union(alt_types))); })() : ((node === "defrecord")) ? register_record(form["name"], form["fields"], env) : ((node === "defunion")) ? register_union(form["name"], form["members"], form["type-params"], form["member-fields"], env) : ((node === "defenum")) ? null : ((node === "defmulti")) ? (env[form["name"]] = make_fn([ANY], null, ANY)) : null; })();
return null; });
return env; })();
}

function check_form(form, env) {
  return (() => { const node = form["node"]; return ((node === "def")) ? (() => { infer_expr(form, env);
return null; })() : ((node === "defonce")) ? (() => { infer_expr(form, env);
return null; })() : ((node === "defn")) ? (() => { infer_expr(form, env);
return null; })() : ((node === "defn-multi")) ? (() => { infer_expr(form, env);
return null; })() : ((node === "defrecord")) ? null : ((node === "defunion")) ? null : ((node === "defenum")) ? null : ((node === "defscalar")) ? null : ((node === "deferror")) ? null : ((node === "protocol")) ? null : ((node === "deftype")) ? null : ((node === "defmulti")) ? null : ((node === "defmethod")) ? null : ((node === "with-meta")) ? check_form(form["expr"], env) : (() => { infer_expr(form, env);
return null; })(); })();
}

function type_check(prog) {
  (() => { const mode = prog["mode"]; return (() => { if ((mode === "strict")) { (STATE["record-fields"] = {});
(STATE["record-field-order"] = {});
(STATE["union-members"] = {});
(STATE["parametric-unions"] = {});
(STATE["diagnostics"] = []);
return (() => { const env = build_initial_env(prog); return prog["forms"].forEach((form) => { check_form(form, env);
return null; }); })(); } })(); })();
  return (() => { const diags = STATE["diagnostics"]; return {["diagnostics"]: diags, ["count"]: diags.length}; })();
}

