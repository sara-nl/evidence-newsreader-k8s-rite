package nl.surfsara.sda.enkr;

import io.github.vmk.rite.operations.Recipe;
import io.github.vmk.rite.operations.Step;
import io.github.vmk.rite.operations.implementations.fileresolution.CopyInOperation;
import io.github.vmk.rite.operations.implementations.fileresolution.CopyOutOperation;
import io.github.vmk.rite.operations.implementations.shell.RunBashScriptOperation;
import io.github.vmk.rite.persistence.mongo.MongoRecipeStore;
import io.github.vmk.rite.relic.Relic;
import io.github.vmk.rite.relic.persistence.mongo.MongoStore;
import io.github.vmk.rite.relic.resolvers.RelicResolver;
import io.github.vmk.rite.relic.resolvers.RelicResolverFactory;
import io.github.vmk.rite.relic.resolvers.ResolverDescriptor;
import io.github.vmk.rite.relic.resolvers.implementations.MinioResolver;
import nl.surfsara.sda.enkr.EnkrMain.PropertyKeys;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

public class EnkrUtils {

    public static ArrayList<RelicPair> uploadRelics(File inputDir, String projectId, Properties enkrProperties) throws Exception {
        // FIXME think about better way to propagate enkrProperties
        ArrayList<RelicPair> relics = new ArrayList<>();

        MongoStore ms = new MongoStore(
                enkrProperties.getProperty(PropertyKeys.RHOST.getKey()),
                Integer.parseInt(enkrProperties.getProperty(PropertyKeys.RPORT.getKey())),
                enkrProperties.getProperty(PropertyKeys.RDBNAME.getKey()),
                projectId + "-relics",
                enkrProperties.getProperty(PropertyKeys.RUSER.getKey()),
                enkrProperties.getProperty(PropertyKeys.RPASS.getKey())
        );

        System.out.println("Uploading from: " + inputDir.getAbsolutePath());
        File[] documentListing = inputDir.listFiles();

        for (File document : documentListing) {
            System.out.println("Processing document: " + document.getName());
            String relicId = UUID.randomUUID().toString();
            String fileName = document.getName();
            Relic r = new Relic(relicId);
            r.setFileName(fileName);

            ResolverDescriptor rd = new ResolverDescriptor(r.getIdentifier(), MinioResolver.ENVIRONMENT);
            rd.setProperty(MinioResolver.DescriptorKeys.MINIOHOST.getKey(), enkrProperties.getProperty(PropertyKeys.MHOST.getKey()));
            rd.setProperty(MinioResolver.DescriptorKeys.BUCKET.getKey(), projectId + "-input");
            rd.setProperty(MinioResolver.DescriptorKeys.ACCESKEY.getKey(), enkrProperties.getProperty(PropertyKeys.MACCESSKEY.getKey()));
            rd.setProperty(MinioResolver.DescriptorKeys.SECRETKEY.getKey(), enkrProperties.getProperty(PropertyKeys.MSECRETKEY.getKey()));
            RelicResolver rr = RelicResolverFactory.getInstance().getResolverForEnvironment(MinioResolver.ENVIRONMENT);

            rr.resolveExternal(r, rd, inputDir);

            ms.putRelic(r);
            ms.putResolverDescriptor(rd);

            // output relics
            String orelicId = UUID.randomUUID().toString();
            String ofileName = document.getName().replaceAll("\\.txt", ".naf");
            Relic or = new Relic(orelicId);
            or.setFileName(ofileName);

            ResolverDescriptor ord = new ResolverDescriptor(or.getIdentifier(), MinioResolver.ENVIRONMENT);
            ord.setProperty(MinioResolver.DescriptorKeys.MINIOHOST.getKey(), enkrProperties.getProperty(PropertyKeys.MHOST.getKey()));
            ord.setProperty(MinioResolver.DescriptorKeys.BUCKET.getKey(), projectId + "-output");
            ord.setProperty(MinioResolver.DescriptorKeys.ACCESKEY.getKey(), enkrProperties.getProperty(PropertyKeys.MACCESSKEY.getKey()));
            ord.setProperty(MinioResolver.DescriptorKeys.SECRETKEY.getKey(), enkrProperties.getProperty(PropertyKeys.MSECRETKEY.getKey()));
            RelicResolver orr = RelicResolverFactory.getInstance().getResolverForEnvironment(MinioResolver.ENVIRONMENT);

            ms.putRelic(or);
            ms.putResolverDescriptor(ord);

            relics.add(new RelicPair(r, or));
        }
        return relics;
    }

    public static void prepareRecipes(ArrayList<RelicPair> relics, String projectId, Properties enkrProperties) throws Exception {
        MongoRecipeStore ms = new MongoRecipeStore(
                enkrProperties.getProperty(PropertyKeys.JHOST.getKey()),
                Integer.parseInt(enkrProperties.getProperty(PropertyKeys.JPORT.getKey())),
                enkrProperties.getProperty(PropertyKeys.JDBNAME.getKey()),
                projectId + "-recipes",
                enkrProperties.getProperty(PropertyKeys.JUSER.getKey()),
                enkrProperties.getProperty(PropertyKeys.JPASS.getKey())
        );

        for (RelicPair rp : relics) {
            String recipeId = projectId + "-" + rp.getInputRelic().getFileName();
            Recipe recipe = new Recipe(recipeId);

            recipe.setTimeout(7200000); // Two hour timeout
            recipe.setResetOnFailure(false);
            recipe.setResetOnTimeout(false);

            Step s = new Step("Resolve input file");
            CopyInOperation cio = new CopyInOperation();
            cio.setRelicId(rp.getInputRelic().getIdentifier());
            s.add(cio);
            recipe.add(s);

            s = new Step("Run pipeline");
            RunBashScriptOperation bco = new RunBashScriptOperation();
            StringBuffer script = new StringBuffer();
            script.append("#!/bin/bash\n");
//            script.append("/newsreader.sh " + rp.getInputRelic().getFileName());
            script.append("cat \"" + rp.getInputRelic().getFileName() + "\"");
            bco.setScript(script.toString());
            s.add(bco);
            recipe.add(s);

            s = new Step("Resolve output file");
            CopyOutOperation coo = new CopyOutOperation();
            coo.setRelicId(rp.getOutputRelic().getIdentifier());
            s.add(coo);
            recipe.add(s);

            ms.putRecipe(recipe);
        }
    }
}
