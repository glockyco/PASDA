package equiv.checking.repositories;

import equiv.checking.models.Instruction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InstructionRepository extends Repository {
    private static final String INSERT_OR_UPDATE = "" +
        "INSERT INTO instruction(" +
        "benchmark, " +
        "method, " +
        "instruction_index, " +
        "instruction, " +
        "position, " +
        "source_file, " +
        "source_line" +
        ") " +
        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
        "ON CONFLICT DO UPDATE SET " +
        "instruction = excluded.instruction, " +
        "position = excluded.position, " +
        "source_file = excluded.source_file, " +
        "source_line = excluded.source_line";

    public static void insertOrUpdate(Iterable<Instruction> instructions) {
        for (Instruction instruction: instructions) {
            insertOrUpdate(instruction);
        }
    }

    public static void insertOrUpdate(Instruction instruction) {
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(INSERT_OR_UPDATE)) {
            ps.setObject(1, instruction.benchmark);
            ps.setObject(2, instruction.method);
            ps.setObject(3, instruction.instructionIndex);
            ps.setObject(4, instruction.instruction);
            ps.setObject(5, instruction.position);
            ps.setObject(6, instruction.sourceFile);
            ps.setObject(7, instruction.sourceLine);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
