{ pkgs, lib, ... }:

let
  script = pkgs.writeShellScriptBin "demo" ''
    #!${pkgs.bash}/bin/bash
    set -e

    NAME="''${USER:-world}"
    items=( ''${list[@]} )

    for n in "''${items[@]}"; do
      printf 'hello %s\n' "$n"
    done
  '';
  plain = ''
    line one
    line two
    line three
  '';
in
{
  packages = [ script ];
  scriptText = plain;
}
