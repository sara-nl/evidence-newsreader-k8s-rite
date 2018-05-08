package nl.surfsara.sda.enkr;

import joptsimple.OptionParser;
import joptsimple.OptionSpec;

import java.io.File;
import java.util.ArrayList;

public class EnkrOptionParser extends OptionParser {
    public OptionSpec<String> projectId;
    public OptionSpec<File> inputDir;
    public OptionSpec<File> outputDir;
    @SuppressWarnings("rawtypes")
    public OptionSpec help;
    @SuppressWarnings("rawtypes")
    public OptionSpec retract;
    @SuppressWarnings("rawtypes")
    public OptionSpec report;
    @SuppressWarnings("rawtypes")
    public OptionSpec get;
    @SuppressWarnings("rawtypes")
    public OptionSpec put;

    public EnkrOptionParser() {
        super();

        // Option declaration
        ArrayList<String> projectIdOption = new ArrayList<String>();
        projectIdOption.add("id");
        projectIdOption.add("projectid");

        ArrayList<String> inputDirectoryOption = new ArrayList<String>();
        inputDirectoryOption.add("i");
        inputDirectoryOption.add("input");

        ArrayList<String> helpCommand = new ArrayList<String>();
        helpCommand.add("h");
        helpCommand.add("?");
        helpCommand.add("help");

        ArrayList<String> retractOption = new ArrayList<String>();
        retractOption.add("retract");
        retractOption.add("delete");

        ArrayList<String> reportOption = new ArrayList<String>();
        reportOption.add("report");
        reportOption.add("progress");

        ArrayList<String> getOption = new ArrayList<String>();
        getOption.add("retrieve");
        getOption.add("get");

        ArrayList<String> putOption = new ArrayList<String>();
        putOption.add("put");
        putOption.add("upload");

        ArrayList<String> outputDirectoryOption = new ArrayList<String>();
        outputDirectoryOption.add("o");
        outputDirectoryOption.add("output");

        // accept rules:
        projectId = acceptsAll(projectIdOption, "Required. The project name. Used as id to store job information and output files.").withRequiredArg().ofType(String.class).describedAs("string");
        inputDir = acceptsAll(inputDirectoryOption, "Optional. The directory to read input files from.").withRequiredArg().ofType(File.class).describedAs("input directory");
        outputDir = acceptsAll(outputDirectoryOption, "Required in combination with get. The directory to write output files to.").withRequiredArg().ofType(File.class).describedAs("output directory");
        retract = acceptsAll(retractOption, "Optional. Retracts or deletes all jobs matching the project name.");
        report = acceptsAll(reportOption, "Optional. Displays a report on job progress. Requires a project name.");
        get = acceptsAll(getOption, "Optional. Gets all the curent output files to the current directory. Requires a project name.");
        put = acceptsAll(putOption, "Optional. Uploads input files and schedules job token. Reguires a project name.");
        help = acceptsAll(helpCommand, "Prints usage information.");
    }
}
