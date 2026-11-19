<#-- @ftlvariable name="parameters" type="differencing.DifferencingParameters" -->
<#-- @ftlvariable name="timeout" type="int" -->
<#-- @ftlvariable name="depthLimit" type="int" -->

target = ${parameters.targetNamespace}.${parameters.targetClassName}
symbolic.method = ${parameters.targetNamespace}.${parameters.targetClassName}.run(${parameters.symbolicParameters})
classpath=target/classes
symbolic.min_int=-100
symbolic.max_int=100
symbolic.min_long=-100
symbolic.max_long=100
symbolic.min_double=-100.0
symbolic.max_double=100.0
symbolic.debug = false
symbolic.optimizechoices = false
symbolic.lazy=on
symbolic.arrays=true
symbolic.strings = true
<#-- Coral decides which branches SPF follows. SPF drops a branch for which Coral finds no input, also a feasible one. -->
symbolic.dp=coral
symbolic.string_dp_timeout_ms=${timeout?string.computer}
search.depth_limit=${depthLimit?string.computer}
search.multiple_errors=true
search.class = .search.CustomSearch
