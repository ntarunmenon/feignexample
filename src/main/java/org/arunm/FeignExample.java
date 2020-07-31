package org.arunm;

import feign.*;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

import java.io.IOException;
import java.util.List;

public class FeignExample {

    public interface FeignClient {
        class Post {
            String id;
            String title;
            String author;

            public Post(String title, String author) {
                this.title = title;
                this.author = author;
            }
            public Post() { }

            @Override
            public String toString() {
                return "Post{" +
                        "id='" + id + '\'' +
                        ", title='" + title + '\'' +
                        ", author='" + author + '\'' +
                        '}';
            }
        }

        @RequestLine("GET /posts")
        List<Post> posts();

        @RequestLine("POST /posts")
        @Headers("Content-Type: application/json")
        void createPosts(Post post);

        static FeignClient connect() {
            final Decoder decoder = new GsonDecoder();
            final Encoder encoder = new GsonEncoder();
            return Feign.builder()
                    .encoder(encoder)
                    .decoder(decoder)
                    .errorDecoder(new FeignClientErrorDecoder(decoder))
                    .logger(new Logger.ErrorLogger())
                    .logLevel(Logger.Level.BASIC)
                    .target(FeignClient.class,"http://localhost:3000");

        }

    }
    public static void main(String[] args) {
        final FeignClient client = FeignClient.connect();

        System.out.println("Getting Posts");
        final List<FeignClient.Post> posts = client.posts();
        posts.forEach(System.out::println);

        System.out.println("Creating Posts");
        client.createPosts(new FeignClient.Post("running goal","Arun"));
    }

    static class FeignClientError extends RuntimeException {
        private String message; // parsed from json

        @Override
        public String getMessage() {
            return message;
        }
    }

    static class FeignClientErrorDecoder implements ErrorDecoder {

        final Decoder decoder;
        final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

        FeignClientErrorDecoder(Decoder decoder) {
            this.decoder = decoder;
        }

        @Override
        public Exception decode(String methodKey, Response response) {
            try {
                // must replace status by 200 other GSONDecoder returns null
                response = response.toBuilder().status(200).build();
                return (Exception) decoder.decode(response, FeignClientError.class);
            } catch (final IOException fallbackToDefault) {
                return defaultDecoder.decode(methodKey, response);
            }
        }
    }
}
