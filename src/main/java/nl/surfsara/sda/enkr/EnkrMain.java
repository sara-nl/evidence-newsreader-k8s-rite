package nl.surfsara.sda.enkr;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

public class EnkrMain {
    private static String PROPERTIES = "enkr.properties";

    public enum PropertyKeys {
        CLIENTID("clientid", ""),
        RHOST("relic.hostname", ""),
        RPORT("relic.port", ""),
        RDBNAME("relic.dbname", ""),
        RAUTH("relic.auth", "false"),
        RUSER("relic.user", ""),
        RPASS("relic.pass", ""),
        JHOST("rite.hostname", ""),
        JPORT("rite.port", ""),
        JDBNAME("rite.dbname", ""),
        JAUTH("rite.auth", "false"),
        JUSER("rite.user", ""),
        JPASS("rite.pass", ""),
        MHOST("minio.host", ""),
        MSECRETKEY("minio.secretkey", ""),
        MACCESSKEY("minio.accesskey", "");

        private final String key;
        private final String defaultValue;

        private PropertyKeys(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String getDefaultValue() {
            if (this == CLIENTID) {
                return UUID.randomUUID().toString();
            }
            return defaultValue;
        }

        public String getKey() {
            return key;
        }

        public String getProperty(Properties properties) {
            return properties.getProperty(this.getKey(), this.getDefaultValue());
        }

    }

    private static Properties properties;

    public static void main(String[] args) {

        CommandMain cm = new CommandMain();
        CommandReport cr = new CommandReport();
        CommandPut cp = new CommandPut();
        String report = "report";
        String put = "put";
        JCommander jc = JCommander.newBuilder()
                .addObject(cm)
                .addCommand(report, cr)
                .addCommand(put, cp)
                .build();
        jc.setProgramName("enkr");
        try {
            jc.parse(args);
        } catch (ParameterException e) {
            showErrorAndExit(jc, e);
        }

        Boolean showUsage = false;
        if (cm.help) {
            showUsage = true;
        }

        System.out.println();
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("Using the following application properties: ");
        properties = new Properties();
        try {
            properties.load(new FileInputStream(new File(PROPERTIES)));
        } catch (Exception e1) {
            // Absorb
            System.out.println("Could not read properties files. Assuming programmed default values for client settings.");
        }
        if (isNullOrEmpty(properties.getProperty(PropertyKeys.CLIENTID.key))) {
            properties.setProperty(PropertyKeys.CLIENTID.key, PropertyKeys.CLIENTID.getDefaultValue());
            try {
                properties.store(new FileOutputStream(new File(PROPERTIES)), "Added auto-generated clientid.");
            } catch (Exception e) {
                showErrorAndExit(jc, e);
            }
        }
        for (PropertyKeys p : PropertyKeys.values()) {
            System.out.println("\t " + p.getKey() + ": " + getProperty(p));
        }
        System.out.println("-------------------------------------------------------------------------------");

        try {
            if (report.equals(jc.getParsedCommand())) {
                String projectid = cr.projectid;
                System.out.println("Gathering job report for project: " + projectid + "...");
                //String regex = ".*" + projectid + "\\].*" + getProperty(PropertyKeys.CLIENTID).replaceAll("-", "\\-") + ".*";
                String regex = projectid + "-.*";
                String host = getProperty(PropertyKeys.JHOST);
                int port = Integer.parseInt(getProperty(PropertyKeys.JPORT));
                String dbName = getProperty(PropertyKeys.JDBNAME);
                Mongo mongo = new Mongo(host, port);
                DB db = mongo.getDB(dbName);
                if (Boolean.parseBoolean(getProperty(PropertyKeys.JAUTH))) {
                    db.authenticate(getProperty(PropertyKeys.JUSER), getProperty(PropertyKeys.JPASS).toCharArray());
                }
                DBCollection recipeCollection = db.getCollection(projectid + "-recipes");
                BasicDBObject q = new BasicDBObject();
                q.put("recipe", new BasicDBObject("$regex", regex));
                long total = recipeCollection.count(q);
                q = new BasicDBObject();
                q.put("recipe", new BasicDBObject("$regex", regex));
                q.put("completed", Boolean.valueOf(true));
                q.put("failed", Boolean.valueOf(false));
                long completed = recipeCollection.count(q);
                q = new BasicDBObject();
                q.put("recipe", new BasicDBObject("$regex", regex));
                q.put("completed", Boolean.valueOf(true));
                q.put("failed", Boolean.valueOf(true));
                long failed = recipeCollection.count(q);
                q = new BasicDBObject();
                q.put("recipe", new BasicDBObject("$regex", regex));
                q.put("completed", Boolean.valueOf(false));
                q.put("clientid", new BasicDBObject("$type", Integer.valueOf(2)));
                long locked = recipeCollection.count(q);
                q = new BasicDBObject();
                q.put("recipe", new BasicDBObject("$regex", regex));
                q.put("completed", Boolean.valueOf(false));
                q.put("clientid", new BasicDBObject("$type", Integer.valueOf(10)));
                long unlocked = recipeCollection.count(q);
                System.out.println("Progress report: ");
                System.out.println((new StringBuilder("Total: ")).append(total).toString());
                System.out.println((new StringBuilder("Completed: ")).append(completed).toString());
                System.out.println((new StringBuilder("Failed: ")).append(failed).toString());
                System.out.println((new StringBuilder("Locked: ")).append(locked).toString());
                System.out.println((new StringBuilder("Unlocked: ")).append(unlocked).toString());
                System.out.println();
                mongo.close();
            } else if (put.equals(jc.getParsedCommand())) {
                String projectid = cp.projectid;
                File inputDir = new File(cp.input);

                System.out.println("Uploading user files...");
                ArrayList<RelicPair> relics = EnkrUtils.uploadRelics(inputDir, projectid, properties);
                System.out.println("Creating jobs for project: " + projectid + "...");
                EnkrUtils.prepareRecipes(relics, projectid, properties);
            } else {
                showUsage = true;
            }
            if (showUsage) {
                jc.usage();
            }
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println("Done!");
            System.out.println("-------------------------------------------------------------------------------");
        } catch (Exception e) {
            showErrorAndExit(jc, e);
        }
    }

    private static void showErrorAndExit(JCommander jc, Exception e) {
        System.out.println("Something didn't quite work like expected: [" + e.getMessage() + "]");
        jc.usage();
        System.exit(1);
    }

    private static String getProperty(PropertyKeys prop) {
        return prop.getProperty(properties);
    }

    private static boolean isNullOrEmpty(String s) {
        if (s == null || "".equals(s)) {
            return true;
        }
        return false;
    }
}
