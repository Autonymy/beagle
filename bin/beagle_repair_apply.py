"""Pure-text edit helpers for the beagle-repair loop.

Extracted from bin/beagle-repair so the apply logic is unit-testable without
running the full diagnostic pipeline. See beagle-test/tests/repair_apply_test.py
and ~/code/life-os/threads/20260615005103-beagle_python_repair_consume_structured.md
"""

import re


def insert_match_clauses(text, anchor_line, clauses):
    """Insert clause skeletons before the closing paren of the (match ...) form
    anchored at anchor_line (1-based).

    The checker hands back throw-bodied skeletons with the correct constructor
    and binder arity, so the result is a well-formed, exhaustive match that
    re-verifies. Single-line matches stay on one line (space-separated);
    multi-line matches get one clause per line at the existing clause indent.

    Returns the new text, or None if the form can't be located/balanced.
    """
    try:
        anchor = int(anchor_line)
    except (TypeError, ValueError):
        return None
    src_lines = text.split('\n')
    if anchor < 1 or anchor > len(src_lines):
        return None
    pre = sum(len(l) + 1 for l in src_lines[:anchor - 1])

    # Find the real `(match` from the anchor, skipping any `(match` that sits
    # inside a string literal (string-aware, so a decoy in a string can't win).
    open_off = None
    j, in_str = pre, False
    while j < len(text):
        c = text[j]
        if in_str:
            if c == '\\':
                j += 2
                continue
            if c == '"':
                in_str = False
        elif c == '"':
            in_str = True
        elif c == '(' and re.match(r'\(\s*match\b', text[j:]):
            open_off = j
            break
        j += 1
    if open_off is None:
        return None

    # Balance parens from the match's '(' to its ')', skipping string contents.
    depth = 0
    in_str = False
    i, n = open_off, len(text)
    close_off = None
    while i < n:
        c = text[i]
        if in_str:
            if c == '\\':
                i += 2
                continue
            if c == '"':
                in_str = False
        elif c == '"':
            in_str = True
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                close_off = i
                break
        i += 1
    if close_off is None:
        return None

    form = text[open_off:close_off]
    if '\n' not in form:
        # Single-line match: keep it on one line, space-separated, so we never
        # emit a stray newline mid-form (which produced broken output before).
        insertion = ''.join(' ' + c for c in clauses)
    else:
        # Multi-line: reuse the indentation of the first existing own-line clause.
        indent = None
        for ln in form.split('\n')[1:]:
            stripped = ln.lstrip()
            if stripped.startswith('['):
                indent = ln[:len(ln) - len(stripped)]
                break
        if indent is None:
            # No own-line clause (all clauses share the (match line): indent one
            # step under the match's own line — NOT the whole line prefix.
            line_start = text.rfind('\n', 0, open_off) + 1
            base = text[line_start:open_off]
            base_ws = base[:len(base) - len(base.lstrip())]
            indent = base_ws + '  '
        insertion = ''.join('\n' + indent + c for c in clauses)
    return text[:close_off] + insertion + text[close_off:]
