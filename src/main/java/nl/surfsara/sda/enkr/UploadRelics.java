package nl.surfsara.sda.enkr;

import java.io.File;
import java.util.UUID;

import io.github.vmk.rite.relic.Relic;
import io.github.vmk.rite.relic.persistence.mongo.MongoStore;
import io.github.vmk.rite.relic.resolvers.RelicResolver;
import io.github.vmk.rite.relic.resolvers.RelicResolverFactory;
import io.github.vmk.rite.relic.resolvers.ResolverDescriptor;
import io.github.vmk.rite.relic.resolvers.implementations.MinioResolver;

public class UploadRelics {

    public static void main(String args[]) throws Exception {

        MongoStore ms = new MongoStore("localhost", 27017, "newsreader-jobs", "relics", "nlesc", "bee-land-wwww-rush");

        String documentPath = args[0];
        System.out.println("Processing: " + documentPath);

        File documentDir = new File(documentPath);
        File[] documentListing = documentDir.listFiles();

        for (File document : documentListing) {
            String relicId = UUID.randomUUID().toString();
            String fileName = document.getName();
            Relic r = new Relic(relicId);
            r.setFileName(fileName);
            ResolverDescriptor rd = new ResolverDescriptor(r.getIdentifier(), MinioResolver.ENVIRONMENT);
            rd.setProperty(MinioResolver.DescriptorKeys.MINIOHOST.getKey(), "newsreader-minio.jove.surfsara.nl");
            rd.setProperty(MinioResolver.DescriptorKeys.BUCKET.getKey(), "input");
            rd.setProperty(MinioResolver.DescriptorKeys.ACCESKEY.getKey(), "nlesc");
            rd.setProperty(MinioResolver.DescriptorKeys.SECRETKEY.getKey(), "bee-land-wwww-rush");
            RelicResolver rr = RelicResolverFactory.getInstance().getResolverForEnvironment(MinioResolver.ENVIRONMENT);
            rr.resolveExternal(r, rd, documentDir);

            //Boolean result = ms.putResolverDescriptor(rd);
            //ms.putRelic(r);
            //System.out.println(document.getName() +" "+ result.toString());
        }

    }

}
