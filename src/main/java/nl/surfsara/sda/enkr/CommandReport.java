package nl.surfsara.sda.enkr;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Display a report on job progress")
public class CommandReport {
    @Parameter (names = "--projectid", required = true)
    public String projectid;
}
