package helloworld;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;

import com.amazonaws.services.logs.*;
import com.amazonaws.services.logs.model.*;

import java.util.List;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);
        try {
            final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.AP_SOUTHEAST_2).build();
            List<Bucket> buckets = s3.listBuckets();
            
            StringBuilder stringBuilder = new StringBuilder();
            for (Bucket b : buckets) {
                stringBuilder.append(b.getName()).append(",");
                System.out.println("HERE " + b.getName());
            }
            String bucketListSt = stringBuilder.toString();
            final String pageContents = this.getPageContents("https://checkip.amazonaws.com");
            String output = String.format("{ \"message\": \"hello world\", \"location\": \"%s\" , \"buckets\": \"%s\" }", pageContents, bucketListSt);
            System.out.println("HERE GETTING 4");


            final AWSLogs awsLogs = AWSLogsClientBuilder.standard().withRegion(Regions.AP_SOUTHEAST_2).build();
            CreateExportTaskRequest request = new CreateExportTaskRequest();
            String startDateSt="01/07/2020";  
            Date startDate = new SimpleDateFormat("dd/MM/yyyy").parse(startDateSt);  
            System.out.println("Start Date : " + startDate.toString());
            String endDateSt="02/07/2020";  
            Date endDate = new SimpleDateFormat("dd/MM/yyyy").parse(endDateSt);  
            System.out.println("End Date : " + endDate.toString());
            String destination = "svstests3bucket";
            String destinationPrefix = "nghitestlogs";
            long from = startDate.getTime();
            String logGroupName= "/sorse/wildfly";
            String logStreamNamePrefix = "svstest-test-wildfly";
            String taskName = "test-export-logs";
            long to = endDate.getTime();

            request.setDestination(destination);
            request.setDestinationPrefix(destinationPrefix);
            request.setFrom(from);
            request.setLogGroupName(logGroupName);
            request.setLogStreamNamePrefix(logStreamNamePrefix);
            request.setTaskName(taskName);
            request.setTo(to);
            CreateExportTaskResult result = awsLogs.createExportTask(request);
            System.out.println("RESULT " + result.toString());

            return response
                    .withStatusCode(200)
                    .withBody(output);
        }
        catch (ParseException e)
        {
            return response
                    .withBody("{ParseException}")
                    .withStatusCode(500);
        }
        catch (IOException e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private String getPageContents(String address) throws IOException{
        URL url = new URL(address);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
