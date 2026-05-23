
const STRING_TAG = "#%string";

const BRACKET_TAG = "#%brackets";

const MAP_TAG = "#%map";

const SET_TAG = "#%set";

const REGEX_TAG = "#%regex";

function whitespace_p(ch) {
  return ((ch === " ") || (ch === "\n") || (ch === "\r") || (ch === "\t"));
}

function newline_p(ch) {
  return ((ch === "\n") || (ch === "\r"));
}

function digit_p(ch) {
  return (() => { const code = ch.charCodeAt(0); return ((code >= 48) && (code <= 57)); })();
}

function delimiter_p(ch) {
  return (whitespace_p(ch) || (ch === "(") || (ch === ")") || (ch === "[") || (ch === "]") || (ch === "{") || (ch === "}") || (ch === "\"") || (ch === ";"));
}

function make_result(value, pos) {
  return {["value"]: value, ["pos"]: pos};
}

function skip_line_comment(src, pos) {
  return (() => { const len = src.length; return (() => { let j = pos; while (true) {
    if (((j >= len) || newline_p(src.charAt(j)))) { return (((j < len) && (src.charAt(j) === "\n")) ? (j + 1) : j); } else { const _recur_0 = (j + 1); j = _recur_0; continue; }
  } })(); })();
}

function skip_ws(src, pos) {
  return (() => { const len = src.length; return (() => { let i = pos; while (true) {
    if ((i >= len)) { return i; } else { const ch = src.charAt(i); if (whitespace_p(ch)) { const _recur_0 = (i + 1); i = _recur_0; continue; } else if ((ch === ";")) { const _recur_0 = skip_line_comment(src, (i + 1)); i = _recur_0; continue; } else { return i; } }
  } })(); })();
}

function decode_escape(ch) {
  return ((ch === "n")) ? "\n" : ((ch === "t")) ? "\t" : ((ch === "r")) ? "\r" : ((ch === "\\")) ? "\\" : ((ch === "\"")) ? "\"" : ch;
}

function read_string_step(src, i, buf) {
  return (() => { const ch = src.charAt(i); return ((ch === "\\") ? (() => { buf.push(decode_escape(src.charAt((i + 1))));
return (i + 2); })() : (() => { buf.push(ch);
return (i + 1); })()); })();
}

function read_string_literal(src, pos) {
  return (() => { const len = src.length; const buf = []; return (() => { let i = (pos + 1); while (true) {
    if ((i >= len)) { return (() => { process.stderr.write("beagle reader: unterminated string\n");
return make_result([STRING_TAG, ""], i); })(); } else if ((src.charAt(i) === "\"")) { return make_result([STRING_TAG, buf.join("")], (i + 1)); } else { const _recur_0 = read_string_step(src, i, buf); i = _recur_0; continue; }
  } })(); })();
}

function count_hashes(src, pos) {
  return (() => { let i = pos; let n = 0; while (true) {
    if (((i < src.length) && (src.charAt(i) === "#"))) { const _recur_0 = (i + 1); const _recur_1 = (n + 1); i = _recur_0; n = _recur_1; continue; } else { return n; }
  } })();
}

function raw_push_quote_hashes(buf, n) {
  buf.push("\"");
  return $$bc.range(n).forEach((j) => {
  buf.push("#");
});;
}

function read_raw_step(src, i, buf, hc) {
  return ((src.charAt(i) === "\"") ? (() => { const found = count_hashes(src, (i + 1)); return ((found >= hc) ? -1 : (() => { raw_push_quote_hashes(buf, found);
return (i + 1 + found); })()); })() : (() => { buf.push(src.charAt(i));
return (i + 1); })());
}

function read_raw_string(src, pos) {
  return (() => { const hc = count_hashes(src, pos); const open_pos = (pos + hc); const len = src.length; return (((open_pos >= len) || (src.charAt(open_pos) !== "\"")) ? (() => { process.stderr.write("beagle reader: expected '\"' after #r hashes\n");
return make_result("", open_pos); })() : (() => { const buf = []; return (() => { let i = (open_pos + 1); while (true) {
    if ((i >= len)) { return make_result([STRING_TAG, buf.join("")], i); } else { const next = read_raw_step(src, i, buf, hc); if ((next === -1)) { return make_result([STRING_TAG, buf.join("")], (i + 1 + hc)); } else { const _recur_0 = next; i = _recur_0; continue; } }
  } })(); })()); })();
}

function read_number(src, pos) {
  return (() => { const len = src.length; return (() => { let i = pos; let has_dot = false; while (true) {
    if ((i >= len)) { return (() => { const text = src.substring(pos, i); return make_result((has_dot ? parseFloat(text) : parseInt(text, 10)), i); })(); } else { const ch = src.charAt(i); if (digit_p(ch)) { const _recur_0 = (i + 1); const _recur_1 = has_dot; i = _recur_0; has_dot = _recur_1; continue; } else if (((ch === ".") && (!has_dot) && ((i + 1) < len) && digit_p(src.charAt((i + 1))))) { const _recur_0 = (i + 1); const _recur_1 = true; i = _recur_0; has_dot = _recur_1; continue; } else { return (() => { const text = src.substring(pos, i); return make_result((has_dot ? parseFloat(text) : parseInt(text, 10)), i); })(); } }
  } })(); })();
}

function read_symbol_text(src, pos) {
  return (() => { const len = src.length; return (() => { let i = pos; while (true) {
    if ((i >= len)) { return make_result(src.substring(pos, i), i); } else { if (delimiter_p(src.charAt(i))) { return make_result(src.substring(pos, i), i); } else { const _recur_0 = (i + 1); i = _recur_0; continue; } }
  } })(); })();
}

function classify_atom(text) {
  return ((text === "true")) ? true : ((text === "false")) ? false : text;
}

function push_and_skip(items, result, src) {
  items.push(result["value"]);
  return skip_ws(src, result["pos"]);
}

function read_delimited(src, pos, close) {
  return (() => { const items = []; return (() => { let p = skip_ws(src, pos); while (true) {
    if ((p >= src.length)) { return (() => { process.stderr.write(("".concat("beagle reader: expected ", close, " before EOF\n")));
return make_result(items, p); })(); } else if ((src.charAt(p) === close)) { return make_result(items, (p + 1)); } else { const result = read_datum(src, p); if ((result == null)) { return make_result(items, p); } else { const _recur_0 = push_and_skip(items, result, src); p = _recur_0; continue; } }
  } })(); })();
}

function read_hash_dispatch(src, pos) {
  return (() => { const len = src.length; return (((pos + 1) >= len) ? make_result("#", (pos + 1)) : (() => { const next = src.charAt((pos + 1)); return ((next === "{")) ? (() => { const result = read_delimited(src, (pos + 2), "}"); return make_result([SET_TAG].concat(result["value"]), result["pos"]); })() : ((next === "\"")) ? (() => { const str_result = read_string_literal(src, (pos + 1)); return make_result([REGEX_TAG, str_result["value"][1]], str_result["pos"]); })() : ((next === "r")) ? read_raw_string(src, (pos + 2)) : (() => { const sym_result = read_symbol_text(src, pos); return make_result(sym_result["value"], sym_result["pos"]); })(); })()); })();
}

function read_datum(src, pos) {
  return (() => { const p = skip_ws(src, pos); const len = src.length; return ((p >= len) ? null : (() => { const ch = src.charAt(p); return ((ch === "(")) ? read_delimited(src, (p + 1), ")") : ((ch === "[")) ? (() => { const result = read_delimited(src, (p + 1), "]"); return make_result([BRACKET_TAG].concat(result["value"]), result["pos"]); })() : ((ch === "{")) ? (() => { const result = read_delimited(src, (p + 1), "}"); return make_result([MAP_TAG].concat(result["value"]), result["pos"]); })() : ((ch === "\"")) ? read_string_literal(src, p) : ((ch === "#")) ? read_hash_dispatch(src, p) : ((ch === "'")) ? (() => { const inner = read_datum(src, (p + 1)); return ((inner == null) ? make_result(["quote", null], (p + 1)) : make_result(["quote", inner["value"]], inner["pos"])); })() : ((ch === "`")) ? (() => { const inner = read_datum(src, (p + 1)); return ((inner == null) ? make_result(["quasiquote", null], (p + 1)) : make_result(["quasiquote", inner["value"]], inner["pos"])); })() : ((ch === "@")) ? (() => { const inner = read_datum(src, (p + 1)); return ((inner == null) ? make_result(["deref", null], (p + 1)) : make_result(["deref", inner["value"]], inner["pos"])); })() : (((ch === "-") && ((p + 1) < len) && digit_p(src.charAt((p + 1))))) ? (() => { const num_result = read_number(src, (p + 1)); const val = num_result["value"]; return make_result((0 - val), num_result["pos"]); })() : (digit_p(ch)) ? read_number(src, p) : (((ch === ":") && (((p + 1) >= len) || delimiter_p(src.charAt((p + 1)))))) ? make_result(":", (p + 1)) : ((ch === ":")) ? (() => { const sym_result = read_symbol_text(src, (p + 1)); return make_result(("".concat(":", sym_result["value"])), sym_result["pos"]); })() : (((ch === ")") || (ch === "]") || (ch === "}"))) ? (() => { process.stderr.write(("".concat("beagle reader: unexpected '", ch, "'\n")));
return null; })() : (() => { const sym_result = read_symbol_text(src, p); const text = sym_result["value"]; return make_result(classify_atom(text), sym_result["pos"]); })(); })()); })();
}

function parse_lang_line(src) {
  return (() => { const len = src.length; return (((len >= 5) && (src.substring(0, 5) === "#lang")) ? (() => { let i = 5; while (true) {
    if (((i >= len) || newline_p(src.charAt(i)))) { return (() => { const lang_text = src.substring(5, i).trim(); const slash_pos = lang_text.indexOf("/"); return {["target"]: ((slash_pos >= 0) ? lang_text.substring((slash_pos + 1)) : null), ["pos"]: (((i < len) && (src.charAt(i) === "\n")) ? (i + 1) : i)}; })(); } else { const _recur_0 = (i + 1); i = _recur_0; continue; }
  } })() : {["target"]: null, ["pos"]: 0}); })();
}

function push_datum_and_skip(datums, result, src) {
  datums.push(result["value"]);
  return skip_ws(src, result["pos"]);
}

function read_all(src) {
  return (() => { const lang_info = parse_lang_line(src); const target = lang_info["target"]; const start_pos = lang_info["pos"]; const datums = []; return (() => { let p = skip_ws(src, start_pos); while (true) {
    if ((p >= src.length)) { return {["target"]: target, ["datums"]: datums}; } else { const result = read_datum(src, p); if ((result == null)) { return {["target"]: target, ["datums"]: datums}; } else { const _recur_0 = push_datum_and_skip(datums, result, src); p = _recur_0; continue; } }
  } })(); })();
}

