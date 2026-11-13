#!/bin/bash
#
# Checks one program pair with PASDA and prints its verdict and partition-level data.
#
# Usage: ./CheckPair.sh <pair> [timeout] [depth-limit]
#
#   <pair>         Directory below ../benchmarks that holds the two versions as oldV.java
#                  and newV.java, in the layout of the benchmark cases, for example
#                  ModDiff/NEq/LoopSub. Its Eq or NEq directory names the expected result.
#                  PASDA stores it next to its verdict and never reads it in the analysis.
#   [timeout]      Seconds for the whole run (default: 300).
#   [depth-limit]  Depth limit of the symbolic execution (default: 10).
#
# The run is added to analysis/results/sqlite.db. The product programs, path conditions
# and symbolic execution logs are written to ../benchmarks/<pair>/instrumented.

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "${SCRIPT_DIR}" || exit 1

DB_PATH="analysis/results/sqlite.db"
DB_CREATE_TABLES_PATH="analysis/create-tables.sql"
PATH_CONDITION_FILTER="path-condition.jq"

BASE_JAR_PATH="build/libs/ARDiff-base-1.0-SNAPSHOT-all.jar"
DIFF_JAR_PATH="build/libs/ARDiff-diff-1.0-SNAPSHOT-all.jar"

if [ $# -lt 1 ] || [ $# -gt 3 ]; then
  sed -n '3,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  exit 2
fi

pair="${1%/}"
timeout="${2:-300}"
depth_limit="${3:-10}"

directory="../benchmarks/${pair}"
oldV="${directory}/oldV.java"
newV="${directory}/newV.java"

if ! [[ "${pair}" =~ ^[^/]+/(Eq|NEq)/[^/]+$|^[^/]+/[^/]+/(Eq|NEq)$ ]]; then
  echo "ERROR: '${pair}' has to consist of three directories, the second or third of which is Eq or NEq."
  exit 2
fi

for file in "${oldV}" "${newV}"; do
  if [ ! -f "${file}" ]; then
    echo "ERROR: The file '${file}' does not exist."
    exit 2
  fi
done

if [ ! -f "${DB_PATH}" ]; then
  touch "${DB_PATH}"
  sqlite3 "${DB_PATH}" < "${DB_CREATE_TABLES_PATH}" > /dev/null
fi

if [ ! -f "${BASE_JAR_PATH}" ]; then
  ./gradlew -PmainClass=Runner.Runner shadowJar
fi

if [ ! -f "${DIFF_JAR_PATH}" ]; then
  ./gradlew -PmainClass=differencing.DifferencingRunner shadowJar
fi

# The base runner aligns the two versions line by line and describes the method under
# analysis in instrumented/IDiffPASDA-Parameters.txt, which the differencing runner reads.

mkdir -p "${directory}/instrumented"

instrumentation_log=$(mktemp)
timeout --foreground "${timeout}s" java -jar "${BASE_JAR_PATH}" --path1 "${oldV}" --path2 "${newV}" --tool P --s coral --b "${depth_limit}" --t "${timeout}" > "${instrumentation_log}" 2>&1

if [ ! -f "${directory}/instrumented/IDiffPASDA-Parameters.txt" ]; then
  echo "ERROR: The two versions could not be prepared for the analysis:"
  cat "${instrumentation_log}"
  rm "${instrumentation_log}"
  exit 1
fi

rm "${instrumentation_log}"

timeout --foreground "${timeout}s" java -jar "${DIFF_JAR_PATH}" "${directory}" PASDA "${timeout}" "${depth_limit}"

# Print the results of the run that was just added to the database.

query() {
  sqlite3 -header -column "${DB_PATH}" "$1"
}

status() {
  echo "CASE $1 WHEN 1 THEN 'SAT' WHEN -1 THEN 'UNSAT' WHEN 0 THEN 'UNKNOWN' ELSE '-' END"
}

covered_lines() {
  echo "(
    SELECT group_concat(source_line, ' ')
    FROM (
      SELECT DISTINCT instruction.source_line
      FROM partition_instruction
      JOIN instruction ON instruction.id = partition_instruction.instruction_id
      WHERE partition_instruction.partition_id = partition.id
        AND partition_instruction.version = $1
      ORDER BY instruction.source_line
    )
  )"
}

run_id=$(sqlite3 "${DB_PATH}" "SELECT max(run_id) FROM settings WHERE tool = 'PASDA-diff';")
result_iteration=$(sqlite3 "${DB_PATH}" "SELECT result_iteration FROM run WHERE id = ${run_id};")

# The verdict of the run is the verdict of this iteration, so its partitions are reported.
result_partitions="
  FROM partition
  JOIN iteration ON iteration.id = partition.iteration_id
  WHERE iteration.run_id = ${run_id}
    AND iteration.iteration = ${result_iteration}"

printf "\nRun\n\n"
query "
  SELECT run.benchmark,
         benchmark.expected,
         run.result,
         run.iteration_count AS iterations,
         run.result_iteration,
         CASE WHEN run.has_timed_out THEN 'yes' ELSE 'no' END AS timed_out,
         round(run.runtime, 1) AS seconds
  FROM run
  JOIN benchmark ON benchmark.benchmark = run.benchmark
  WHERE run.id = ${run_id};"

printf "\nIterations\n\n"
query "
  SELECT iteration,
         result,
         CASE WHEN has_uif THEN 'yes' ELSE 'no' END AS abstracted,
         partition_count AS partitions,
         round(runtime, 1) AS seconds
  FROM iteration
  WHERE run_id = ${run_id}
  ORDER BY iteration;"

printf "\nPartitions of iteration %s\n\n" "${result_iteration}"
query "
  SELECT partition.partition,
         partition.result,
         $(status partition.pc_status) AS pc,
         $(status partition.neq_status) AS neq,
         $(status partition.eq_status) AS eq,
         coalesce($(covered_lines 1), '-') AS lines_v1,
         coalesce($(covered_lines 2), '-') AS lines_v2
  ${result_partitions}
  ORDER BY partition.partition;"

printf "\nThe columns pc, neq and eq hold the answers of the solver to three questions: Is the\n"
printf "path reachable? Can its outputs differ? Can its outputs be equal? The lines are those\n"
printf "of IoldVPASDA%s.java and InewVPASDA%s.java in the directory\n" "${result_iteration}" "${result_iteration}"
printf "%s/instrumented.\n" "${directory}"

models=$(sqlite3 "${DB_PATH}" "SELECT count(*) ${result_partitions} AND partition.neq_model IS NOT NULL;")

if [ "${models}" -gt 0 ]; then
  printf "\nInputs for which the solver found different outputs in iteration %s\n\n" "${result_iteration}"
  query "
    SELECT partition.partition,
           partition.neq_result_v1 AS output_v1,
           partition.neq_result_v2 AS output_v2,
           replace(partition.neq_model, char(10), '') AS model
    ${result_partitions}
      AND partition.neq_model IS NOT NULL
    ORDER BY partition.partition;"
fi

printf "\nPath conditions of iteration %s\n\n" "${result_iteration}"
partitions=$(sqlite3 "${DB_PATH}" "SELECT partition.partition ${result_partitions} ORDER BY partition.partition;")

for partition in ${partitions}; do
  file="${directory}/instrumented/IDiffPASDA${result_iteration}-P${partition}-JSON-PC.json"
  if [ -f "${file}" ]; then
    # The first conjunct follows the number of the partition, the others are indented.
    jq -r -f "${PATH_CONDITION_FILTER}" "${file}" \
      | sed -e "1s/^/$(printf '%-3s' "${partition}")/" -e '2,$s/^/   \&\& /'
  fi
done
