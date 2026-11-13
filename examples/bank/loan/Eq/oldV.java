package demo.benchmarks.bank.loan.Eq;

public class oldV {
  public static int snippet(int income, int debt, int amount) {
    double risk = (3.0 * debt - income) / 10.0;
    double p = 1 / (1 + Math.exp(risk));
    if (p >= 0.8) return Math.min(amount, 60);
    if (p >= 0.5) return Math.min(amount, 5);
    return 0;
  }
}
