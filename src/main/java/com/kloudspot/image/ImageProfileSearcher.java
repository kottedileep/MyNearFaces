// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.kloudspot.image;

import com.yahoo.prelude.query.WordItem;
import com.yahoo.prelude.query.NearestNeighborItem;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.Searcher;
import com.yahoo.search.result.Hit;
import com.yahoo.search.searchchain.Execution;
import com.yahoo.tensor.Tensor; 
import com.yahoo.tensor.TensorType;
import com.yahoo.search.result.FeatureData;
import java.util.logging.Logger;



public class ImageProfileSearcher extends Searcher {

    private static final Logger logger = Logger.getLogger(ImageProfileSearcher.class.getName());

    @Override
    public Result search(Query query, Execution execution) {

        logger.info("Searching .....    ");

        String inputString = query.properties().getString("imagevector");
        Tensor face_embedding = Tensor.from(TensorType.fromSpec("tensor<float>(x[256])"), inputString);

        Query searchQuery = new Query();

        NearestNeighborItem nearestNeighbor = new NearestNeighborItem("image_vector", "query_vector");

        nearestNeighbor.setAllowApproximate(false);
        nearestNeighbor.setTargetNumHits(10);
        nearestNeighbor.setHnswExploreAdditionalHits(200);

        searchQuery.getModel().getQueryTree().setRoot(nearestNeighbor);
        searchQuery.setHits(10);
        searchQuery.getRanking().getFeatures().put("query(query_vector)", face_embedding);
        searchQuery.getRanking().setProfile("image_similarity");

        logger.info("Builded Search Query is  : "+ searchQuery);

        Result result =  execution.search(searchQuery);

        logger.info("validate all hits with relavance");

        // Check the Hits with relavance and return human id and matched hits back as response.
        if(result.getTotalHitCount() == 0 || result.hits().getErrorHit() != null){
            return result;
        }
        else{
            int hitsFound = result.hits().size();
            int matchedHits=0,matchedHumanId=-1;
            for(int i=0;i<hitsFound;i++){
                Hit hit = result.hits().get(0);
                FeatureData featureData = (FeatureData)hit.getField("matchfeatures");
                if(featureData == null){
                       break;
                }
                double closeness = featureData.getDouble("closeness(field,image_vector)");
                int humanid = featureData.getDouble("attribute(human_id)").intValue();
                if(closeness>0.60){
                    if(i==0){
                        matchedHumanId=humanid;
                    }
                    matchedHits++;
                }
                result.hits().remove(0);
            }
            Hit similarImageHits = new Hit("Image Similarity Response");
            similarImageHits.setField("Human ID", matchedHumanId);
            similarImageHits.setField("Matched Hits", matchedHits);
            result.hits().add(similarImageHits);
        }
        return result;
    }
}
