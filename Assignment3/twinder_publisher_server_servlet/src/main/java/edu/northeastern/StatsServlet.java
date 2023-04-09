package edu.northeastern;

import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import edu.northeastern.models.GetStatsResponse;
import edu.northeastern.models.MessageResponse;
import edu.northeastern.models.SwipeDetailsMessage;
import edu.northeastern.utils.MongoDBConnection;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static edu.northeastern.utils.Constants.MONGO_DB_COLLECTION;
import static edu.northeastern.utils.Constants.MONGO_DB_NAME;
import static edu.northeastern.utils.ServletHelper.responseHandler;

@WebServlet(name = "StatsServlet", value = "/StatsServlet")
@Slf4j
public class StatsServlet extends HttpServlet {

    private final Gson gson = new Gson();

    private final MongoClient mongoClient = MongoDBConnection.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("Executing StatsServlet doGet");

        // check we have a URL!
        final String urlPath = request.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) {
            MessageResponse messageResponse = MessageResponse.builder()
                    .message("The given input is invalid.")
                    .build();
            responseHandler(response, HttpServletResponse.SC_BAD_REQUEST, gson.toJson(messageResponse));
            return;
        }
        final String[] urlParts = urlPath.split("/");
        if (!isUrlValid(urlParts)) {
            MessageResponse messageResponse = MessageResponse.builder()
                    .message("The given input is invalid.")
                    .build();
            responseHandler(response, HttpServletResponse.SC_BAD_REQUEST, gson.toJson(messageResponse));
            return;
        }

        final String swiperId = urlParts[1];

        final MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);
        final MongoCollection<SwipeDetailsMessage> collection = database.getCollection(MONGO_DB_COLLECTION, SwipeDetailsMessage.class);

        final Bson filter = Filters.eq("swiper", swiperId);
        final List<SwipeDetailsMessage> queriedMessages = new LinkedList<>();
        collection.find(filter).forEach(queriedMessages::add);

        log.info("Queried {} collection with swiperId: {} and returned {} results.", MONGO_DB_COLLECTION, swiperId, queriedMessages.size());

        // check if the user exist
        if (queriedMessages.isEmpty()) {
            final MessageResponse messageResponse = MessageResponse.builder()
                    .message("The user doesn't exist.")
                    .build();
            responseHandler(response, HttpServletResponse.SC_NOT_FOUND, gson.toJson(messageResponse));
            return;
        }

        final GetStatsResponse getStatsResponse = GetStatsResponse.builder()
                .numLlikes(queriedMessages.stream().map(SwipeDetailsMessage::getLeftOrRight).filter(e -> e.equals("right")).count())
                .numDislikes(queriedMessages.stream().map(SwipeDetailsMessage::getLeftOrRight).filter(e -> e.equals("left")).count())
                .build();
        responseHandler(response, HttpServletResponse.SC_OK, gson.toJson(getStatsResponse));
    }

    private boolean isUrlValid(String[] parts) {
        // get request url: localhost:9988/dev-server/stats/123
        // parts: [, 123]
        return parts.length == 2;
    }
}
