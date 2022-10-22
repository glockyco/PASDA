#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

BASE_JAR_PATH="${SCRIPT_DIR}/build/libs/ARDiff-base-1.0-SNAPSHOT-all.jar"
DIFF_JAR_PATH="${SCRIPT_DIR}/build/libs/ARDiff-diff-1.0-SNAPSHOT-all.jar"

# Configuration settings

dry_run=false

clean=false
build=true

timeout="300" # seconds

run_base=true
run_diff=true

print_cleaned=true
print_skipped=false
print_commands=true

benchmarks=(
  "../benchmarks/Airy/MAX/Eq"
  "../benchmarks/Airy/MAX/NEq"
  "../benchmarks/Airy/Sign/Eq"
  "../benchmarks/Airy/Sign/NEq"
  "../benchmarks/Bess/SIGN/Eq"
  "../benchmarks/Bess/SIGN/NEq"
  "../benchmarks/Bess/SQR/Eq"
  "../benchmarks/Bess/SQR/NEq"
  "../benchmarks/Bess/bessi/Eq"
  "../benchmarks/Bess/bessi/NEq"
  "../benchmarks/Bess/bessi0/Eq"
  "../benchmarks/Bess/bessi0/NEq"
  "../benchmarks/Bess/bessi1/Eq"
  "../benchmarks/Bess/bessi1/NEq"
  "../benchmarks/Bess/bessj/Eq"
  "../benchmarks/Bess/bessj/NEq"
  "../benchmarks/Bess/bessj0/Eq"
  "../benchmarks/Bess/bessj0/NEq"
  "../benchmarks/Bess/bessj1/Eq"
  "../benchmarks/Bess/bessj1/NEq"
  "../benchmarks/Bess/bessk/Eq"
  "../benchmarks/Bess/bessk/NEq"
  "../benchmarks/Bess/bessk0/Eq"
  "../benchmarks/Bess/bessk0/NEq"
  "../benchmarks/Bess/bessk1/Eq"
  "../benchmarks/Bess/bessk1/NEq"
  "../benchmarks/Bess/bessy/Eq"
  "../benchmarks/Bess/bessy/NEq"
  "../benchmarks/Bess/bessy0/Eq"
  "../benchmarks/Bess/bessy0/NEq"
  "../benchmarks/Bess/bessy1/Eq"
  "../benchmarks/Bess/bessy1/NEq"
  "../benchmarks/Bess/dawson/Eq"
  "../benchmarks/Bess/dawson/NEq"
  "../benchmarks/Bess/probks/Eq"
  "../benchmarks/Bess/probks/NEq"
  "../benchmarks/Bess/pythag/Eq"
  "../benchmarks/Bess/pythag/NEq"
  "../benchmarks/Ell/brent/Eq"
  "../benchmarks/Ell/brent/NEq"
  "../benchmarks/Ell/dbrent/Eq"
  "../benchmarks/Ell/dbrent/NEq"
  "../benchmarks/Ell/ell/Eq"
  "../benchmarks/Ell/ell/NEq"
  "../benchmarks/Ell/ellpi/Eq"
  "../benchmarks/Ell/ellpi/NEq"
  "../benchmarks/Ell/plgndr/Eq"
  "../benchmarks/Ell/plgndr/NEq"
  "../benchmarks/Ell/rc/Eq"
  "../benchmarks/Ell/rc/NEq"
  "../benchmarks/Ell/rd/Eq"
  "../benchmarks/Ell/rd/NEq"
  "../benchmarks/Ell/rf/Eq"
  "../benchmarks/Ell/rf/NEq"
  "../benchmarks/Ell/rj/Eq"
  "../benchmarks/Ell/rj/NEq"
  "../benchmarks/Ell/zbrent/Eq"
  "../benchmarks/Ell/zbrent/NEq"
  "../benchmarks/ModDiff/Eq/Add"
  "../benchmarks/ModDiff/Eq/Comp"
  "../benchmarks/ModDiff/Eq/Const"
  "../benchmarks/ModDiff/Eq/LoopMult10"
  "../benchmarks/ModDiff/Eq/LoopMult15"
  "../benchmarks/ModDiff/Eq/LoopMult2"
  "../benchmarks/ModDiff/Eq/LoopMult20"
  "../benchmarks/ModDiff/Eq/LoopMult5"
  "../benchmarks/ModDiff/Eq/LoopSub"
  "../benchmarks/ModDiff/Eq/LoopUnreach10"
  "../benchmarks/ModDiff/Eq/LoopUnreach15"
  "../benchmarks/ModDiff/Eq/LoopUnreach2"
  "../benchmarks/ModDiff/Eq/LoopUnreach20"
  "../benchmarks/ModDiff/Eq/LoopUnreach5"
  "../benchmarks/ModDiff/Eq/Sub"
  "../benchmarks/ModDiff/Eq/UnchLoop"
  "../benchmarks/ModDiff/NEq/LoopMult10"
  "../benchmarks/ModDiff/NEq/LoopMult15"
  "../benchmarks/ModDiff/NEq/LoopMult2"
  "../benchmarks/ModDiff/NEq/LoopMult20"
  "../benchmarks/ModDiff/NEq/LoopMult5"
  "../benchmarks/ModDiff/NEq/LoopSub"
  "../benchmarks/ModDiff/NEq/LoopUnreach10"
  "../benchmarks/ModDiff/NEq/LoopUnreach15"
  "../benchmarks/ModDiff/NEq/LoopUnreach2"
  "../benchmarks/ModDiff/NEq/LoopUnreach20"
  "../benchmarks/ModDiff/NEq/LoopUnreach5"
  "../benchmarks/ModDiff/NEq/UnchLoop"
  "../benchmarks/Ran/bnldev/Eq"
  "../benchmarks/Ran/bnldev/NEq"
  "../benchmarks/Ran/expdev/Eq"
  "../benchmarks/Ran/expdev/NEq"
  "../benchmarks/Ran/gamdev/Eq"
  "../benchmarks/Ran/gamdev/NEq"
  "../benchmarks/Ran/gammln/Eq"
  "../benchmarks/Ran/gammln/NEq"
  "../benchmarks/Ran/gasdev/Eq"
  "../benchmarks/Ran/gasdev/NEq"
  "../benchmarks/Ran/poidev/Eq"
  "../benchmarks/Ran/poidev/NEq"
  "../benchmarks/Ran/ran/Eq"
  "../benchmarks/Ran/ran/NEq"
  "../benchmarks/Ran/ranzero/Eq"
  "../benchmarks/Ran/ranzero/NEq"
  "../benchmarks/caldat/badluk/Eq"
  "../benchmarks/caldat/badluk/NEq"
  "../benchmarks/caldat/julday/Eq"
  "../benchmarks/caldat/julday/NEq"
  "../benchmarks/dart/test/Eq"
  "../benchmarks/dart/test/NEq"
  "../benchmarks/gam/betacf/Eq"
  "../benchmarks/gam/betacf/NEq"
  "../benchmarks/gam/betai/Eq"
  "../benchmarks/gam/betai/NEq"
  "../benchmarks/gam/ei/Eq"
  "../benchmarks/gam/ei/NEq"
  "../benchmarks/gam/erfcc/Eq"
  "../benchmarks/gam/erfcc/NEq"
  "../benchmarks/gam/expint/Eq"
  "../benchmarks/gam/expint/NEq"
  "../benchmarks/gam/gammp/Eq"
  "../benchmarks/gam/gammp/NEq"
  "../benchmarks/gam/gammq/Eq"
  "../benchmarks/gam/gammq/NEq"
  "../benchmarks/gam/gcf/Eq"
  "../benchmarks/gam/gcf/NEq"
  "../benchmarks/gam/gser/Eq"
  "../benchmarks/gam/gser/NEq"
  "../benchmarks/power/test/Eq"
  "../benchmarks/power/test/NEq"
  "../benchmarks/sine/mysin/Eq"
  "../benchmarks/sine/mysin/NEq"
  "../benchmarks/tcas/NonCrossingBiasedClimb/Eq"
  "../benchmarks/tcas/NonCrossingBiasedClimb/NEq"
  "../benchmarks/tcas/NonCrossingBiasedDescend/Eq"
  "../benchmarks/tcas/NonCrossingBiasedDescend/NEq"
  "../benchmarks/tcas/altseptest/Eq"
  "../benchmarks/tcas/altseptest/NEq"
  "../benchmarks/tsafe/conflict/Eq"
  "../benchmarks/tsafe/conflict/NEq"
  "../benchmarks/tsafe/normAngle/Eq"
  "../benchmarks/tsafe/normAngle/NEq"
  "../benchmarks/tsafe/snippet/Eq"
  "../benchmarks/tsafe/snippet/NEq"
)

tool_names=(
  "SE"
  "DSE"
#  "Imp"
#  "ARDiffR"
#  "ARDiffH3"
  "ARDiff"
)

configurations=(
  "--tool S --s coral --b 3"
  "--tool D --s coral --b 3"
#  "--tool I --s coral --b 3"
#  "--tool A --s coral --b 3 --H R"
#  "--tool A --s coral --b 3 --H H3"
  "--tool A --s coral --b 3 --H H123"
)

# Remove results from previous runs

if [ "$clean" = true ] ; then
  for d1 in ../benchmarks/* ; do
    for d2 in "$d1"/* ; do
      for d3 in "$d2"/* ; do
        if [[ ! " ${benchmarks[*]} " =~ " ${d3} " ]]; then
          continue
        fi

        if [ "$print_cleaned" = true ] ; then
          printf "Cleaning %s ...\n" "${d3}"
          echo "rm -rf ${d3}/*/"
        fi

        if [ "$dry_run" = false ] ; then
          rm -rf "${d3:?}"/*/
        fi
      done
    done
  done
fi

# Build the application JAR files

if [ "$build" = true ] ; then
  printf "Building JAR files ..."

  # Build base JAR
  command="gradle -PmainClass=Runner.Runner shadowJar"

  if [ "$print_commands" = true ] ; then
    printf "\n%s" "${command}"
  fi

  if [ "$dry_run" = false ] ; then
    eval "${command}"
  fi

  # Build diff JAR
  command="gradle -PmainClass=differencing.DifferencingRunner shadowJar"

  if [ "$print_commands" = true ] ; then
    printf "\n%s" "${command}"
  fi

  if [ "$dry_run" = false ] ; then
    eval "${command}"
  fi

  printf "\n"
fi

# Process the benchmark programs

for d1 in ../benchmarks/* ; do
  for d2 in "$d1"/* ; do
    for d3 in "$d2"/* ; do
      if [[ ! " ${benchmarks[*]} " =~ " ${d3} " ]]; then
        if [ "$print_skipped" = true ] ; then
          printf "Skipping %s ...\n" "${d3}"
        fi
        continue
      fi

      if [ "$run_base" = true ] || [ "$run_diff" = true ] ; then
        printf "Processing %s ..." "${d3}"

        for i in "${!configurations[@]}" ; do
          # Run base tool(s)
          if [ "$run_base" = true ] ; then
            oldV="${d3}/oldV.java"
            newV="${d3}/newV.java"

            base_command="timeout --verbose --foreground ${timeout}s java -jar '${BASE_JAR_PATH}' --path1 ${oldV} --path2 ${newV} ${configurations[$i]}"

            if [ "$print_commands" = true ] ; then
              printf "\n%s" "${base_command}"
            fi

            if [ "$dry_run" = false ] ; then
              printf "\n"
              mkdir -p "${d3}/instrumented/"
              eval "${base_command}"
            fi
          fi

          # Run diff tool(s)
          if [ "$run_diff" = true ] ; then
            diff_command="timeout --verbose --foreground ${timeout}s java -jar '${DIFF_JAR_PATH}' ${d3} ${tool_names[$i]} 30"

            if [ "$print_commands" = true ] ; then
              printf "\n%s" "${diff_command}"
            fi

            if [ "$dry_run" = false ] ; then
              printf "\n"
              mkdir -p "${d3}/instrumented/"
              eval "${diff_command}"
            fi
          fi
        done

        printf "\n"
      fi

    done
  done
done
