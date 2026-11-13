# Renders a path condition that PASDA wrote as JSON (IDiff*-P<n>-JSON-PC.json)
# as text with one conjunct per line: jq -r -f path-condition.jq <file>

# SPF names a symbolic input <parameter>_<n>_SYMINT or <parameter>_<n>_SYMREAL.
def input_name: sub("_[0-9]+_SYM(INT|REAL)$"; "");

# The conjuncts of a path condition, which is a left-nested chain of "&".
def conjuncts:
  if . == null then empty
  elif ._type == "Operation" and .op.symbol == "&" then (.left | conjuncts), (.right | conjuncts)
  else . end;

# SPF introduces a variable REAL_<n> or INT_<n> for every type cast and binds it
# with a conjunct "REAL_<n> == <operand>".
def is_cast:
  ._type == "Operation" and .op.symbol == "=="
  and (.left._type | test("^Variable")) and (.left.name | test("^(REAL|INT)_[0-9]+$"));

def render:
  if . == null then "null"
  elif ._type == "Operation" then
    if .right == null then "\(.op.symbol)(\(.left | render))"
    else "(\(.left | render) \(.op.symbol) \(.right | render))" end
  elif ._type | test("Function$") then "\(.name)(\(.args | map(render) | join(", ")))"
  elif ._type | test("^Variable") then .name | input_name
  elif ._type | test("^Constant") then .value | tostring
  else tojson end;

[conjuncts] as $all
| ($all | map(select(is_cast) | {key: .left.name, value: .right}) | from_entries) as $casts
| $all
| map(select(is_cast | not))
| map(walk(if type == "object" and (._type // "" | test("^Variable")) and $casts[.name] != null then $casts[.name] else . end))
| map(render)
# Both versions add their branch conditions, so shared conditions occur twice.
| reduce .[] as $conjunct ([]; if index($conjunct) != null then . else . + [$conjunct] end)
| if length == 0 then "true" else .[] end
