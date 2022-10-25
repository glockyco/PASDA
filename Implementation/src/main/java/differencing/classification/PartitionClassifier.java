package differencing.classification;

import com.microsoft.z3.Status;

public class PartitionClassifier implements Classifier {
    private final boolean isMissing;
    private final boolean isBaseToolMissing;
    private final boolean isError;
    private final boolean isTimeout;
    private final boolean isDepthLimited;
    private final Status pcStatus;
    private final Status neqStatus;
    private final Status eqStatus;
    private final boolean hasUif;
    private final boolean hasUifPc;

    public PartitionClassifier(
        boolean isMissing,
        boolean isBaseToolMissing,
        boolean isError,
        boolean isTimeout,
        boolean isDepthLimited,
        Status pcStatus,
        Status neqStatus,
        Status eqStatus,
        boolean hasUifPc,
        boolean hasUifV1,
        boolean hasUifV2
    ) {
        // Partitions should never have a MISSING or BASE_TOOL_MISSING status.
        // This is because we don't know (and can't know) which partitions
        // should or shouldn't exist for any given run.
        assert !isMissing;
        assert !isBaseToolMissing;

        this.isMissing = false;
        this.isBaseToolMissing = false;
        this.isError = isError;
        this.isTimeout = isTimeout;
        this.isDepthLimited = isDepthLimited;
        this.pcStatus = pcStatus;
        this.neqStatus = neqStatus;
        this.eqStatus = eqStatus;
        this.hasUif = hasUifPc || hasUifV1 || hasUifV2;
        this.hasUifPc = hasUifPc;
    }

    @Override
    public Classification getClassification() {
        if (this.isMissing()) {
            return Classification.MISSING;
        } else if (this.isBaseToolMissing()) {
            return Classification.BASE_TOOL_MISSING;
        } else if (this.isError()) {
            return Classification.ERROR;
        } else if (this.isUnreachable()) {
            return Classification.UNREACHABLE;
        } else if (this.isTimeout()) {
            return Classification.TIMEOUT;
        } else if (this.isDepthLimited()) {
            return Classification.DEPTH_LIMITED;
        } else if (this.isUnknown()) {
            return Classification.UNKNOWN;
        } else if (this.isMaybeNeq()) {
            return Classification.MAYBE_NEQ;
        } else if (this.isMaybeEq()) {
            return Classification.MAYBE_EQ;
        } else if (this.isNeq()) {
            return Classification.NEQ;
        } else if (this.isEq()) {
            return Classification.EQ;
        }
        throw new RuntimeException("Unable to classify partition.");
    }

    @Override
    public boolean isMissing() {
        return this.isMissing;
    }

    @Override
    public boolean isBaseToolMissing() {
        return this.isBaseToolMissing;
    }

    @Override
    public boolean isError() {
        return this.isError;
    }

    @Override
    public boolean isUnreachable() {
        return this.pcStatus == Status.UNSATISFIABLE;
    }

    @Override
    public boolean isTimeout() {
        return this.isTimeout;
    }

    @Override
    public boolean isDepthLimited() {
        return this.isDepthLimited;
    }

    @Override
    public boolean isUnknown() {
        // The solver was unable to provide a sat or unsat answer for the query.
        // Thus, we can't say whether the two versions are equivalent or not.
        return this.pcStatus == Status.UNKNOWN || this.neqStatus == Status.UNKNOWN || this.eqStatus == Status.UNKNOWN;
    }

    @Override
    public boolean isMaybeNeq() {
        // MAYBE_NEQ (or maybe EQ):
        // Equivalence checking found the two programs to be NEQ, but:
        // (i) there were uninterpreted functions in the solver query AND
        // (ii) at least one input assignment exists for which the programs are EQ.
        // Thus, the base programs without uninterpreted functions might actually
        // be EQ rather than NEQ if the NEQ results only arise due to the
        // introduction of uninterpreted functions.
        return this.neqStatus == Status.SATISFIABLE && this.eqStatus == Status.SATISFIABLE;
    }

    @Override
    public boolean isMaybeEq() {
        // MAYBE_EQ (or maybe UNREACHABLE):
        // Equivalence checking found the two programs to be EQ, but there were
        // uninterpreted functions in the path condition. Thus, the corresponding
        // partition of the base program without uninterpreted functions might
        // actually be UNREACHABLE rather than EQ.
        return this.neqStatus == Status.UNSATISFIABLE && this.hasUifPc;
    }

    @Override
    public boolean isNeq() {
        return (this.neqStatus == Status.SATISFIABLE && !this.hasUif) || this.eqStatus == Status.UNSATISFIABLE;
    }

    @Override
    public boolean isEq() {
        return this.neqStatus == Status.UNSATISFIABLE && !this.hasUifPc;
    }
}
