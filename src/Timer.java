public class Timer {
    private double totalTime = 0.0;

    public double getTotalTime() {
        return totalTime;
    }

    public void addTime(double time) {
        totalTime += time;
    }
}
