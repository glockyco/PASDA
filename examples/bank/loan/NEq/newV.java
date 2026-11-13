package demo.benchmarks.bank.loan.NEq;

public class newV {
  public static int snippet(int income, int debt, int amount) {
    final int FULL = 50;
    final int PARTIAL = 5;
    double risk = (3.0 * debt - income) / 10.0;
    double p = 1 / (1 + Math.exp(risk));
    if (p >= 0.8) return Math.min(amount, FULL);
    if (p >= 0.5) return Math.min(amount, PARTIAL);
    return 0;
  }
}
