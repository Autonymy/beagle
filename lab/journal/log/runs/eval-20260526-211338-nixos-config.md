# eval-roundtrip — nixos-config @ 914251f

- timestamp:   2026-05-26T21:13:38+07:00
- beagle-sha:  2b2f656
- corpus:      /home/tom/code/nixos-config
- corpus-sha:  914251f

## twin build

| metric | count |
|---|---|
| nix files     | 217 |
| convert-fail  | 0 |
| build-fail    | 0 |

## per-host drvPath equivalence (primary signal)

| metric | count |
|---|---|
| hosts total       | 2 |
| PASS              | 0 |
| FAIL-DIVERGE      | 0 |
| FAIL-TWIN-EVAL    | 2 |
| FAIL-ORIG-EVAL    | 0 |

### FAIL-TWIN-EVAL hosts (converter broke evaluability)

- thinkpad-x1e
- whiterabbit

#### twin eval errors (actionable signal for converter bugs)

```
--- thinkpad-x1e: twin eval failed ---
error:
       … while calling the 'seq' builtin
         at /nix/store/ds6zw8983xm46y10dq7aswqkgmry968q-source/lib/modules.nix:361:18:
          360|         options = checked options;
          361|         config = checked (removeAttrs config [ "_module" ]);
             |                  ^
          362|         _module = checked (config._module);

       … while evaluating a branch condition
         at /nix/store/ds6zw8983xm46y10dq7aswqkgmry968q-source/lib/modules.nix:297:9:
          296|       checkUnmatched =
          297|         if config._module.check && config._module.freeformType == null && merged.unmatchedDefns != [ ] then
             |         ^
          298|           let

       (stack trace truncated; use '--show-trace' to show the full, detailed trace)

       error: syntax error, unexpected invalid token
       at /nix/store/g5kg3jldhj7zn9v6gvfiiwp0dxk00nms-modules/firefox/palefox.nix:17:19:
           16|             file = {
           17|               "${(\".mozilla/firefox/\" + username + \"/chrome\")}".source = config.lib.file.mkOutOfStoreSymlink "/home/${username}/code/palefox/chrome";
             |                   ^
           18|             };
--- whiterabbit: twin eval failed ---
error:
       … while calling the 'seq' builtin
         at /nix/store/ds6zw8983xm46y10dq7aswqkgmry968q-source/lib/modules.nix:361:18:
          360|         options = checked options;
          361|         config = checked (removeAttrs config [ "_module" ]);
             |                  ^
          362|         _module = checked (config._module);

       … while evaluating a branch condition
         at /nix/store/ds6zw8983xm46y10dq7aswqkgmry968q-source/lib/modules.nix:297:9:
          296|       checkUnmatched =
          297|         if config._module.check && config._module.freeformType == null && merged.unmatchedDefns != [ ] then
             |         ^
          298|           let

       (stack trace truncated; use '--show-trace' to show the full, detailed trace)

       error: syntax error, unexpected invalid token
       at /nix/store/g5kg3jldhj7zn9v6gvfiiwp0dxk00nms-modules/firefox/palefox.nix:17:19:
           16|             file = {
           17|               "${(\".mozilla/firefox/\" + username + \"/chrome\")}".source = config.lib.file.mkOutOfStoreSymlink "/home/${username}/code/palefox/chrome";
             |                   ^
           18|             };
```

