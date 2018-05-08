package nl.surfsara.sda.enkr;

import io.github.vmk.rite.relic.Relic;

public class RelicPair {
    public Relic getOutputRelic() {
        return outputRelic;
    }

    public void setOutputRelic(Relic outputRelic) {
        this.outputRelic = outputRelic;
    }

    public Relic getInputRelic() {
        return inputRelic;
    }

    public void setInputRelic(Relic inputRelic) {
        this.inputRelic = inputRelic;
    }

    private Relic inputRelic;
    private Relic outputRelic;

    public RelicPair(Relic inputRelic, Relic outputRelic) {
        this.inputRelic = inputRelic;
        this.outputRelic = outputRelic;
    }
}
