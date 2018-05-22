package nl.surfsara.sda.enkr;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Uploads documents and schedules jobs")
public class CommandPut {
    @Parameter (names = "--projectid", required = true)
    public String projectid;

    @Parameter (names = "--input", required = true)
    public String input;
}
