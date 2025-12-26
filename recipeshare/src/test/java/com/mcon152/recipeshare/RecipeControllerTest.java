package com.mcon152.recipeshare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcon152.recipeshare.web.RecipeController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private static ObjectMapper mapper;

    @BeforeAll
    static void setup() {
        mapper = new ObjectMapper();
    }

    // Internal class for creation-related tests
    @Nested
    class CreationTests {
        @Test
        void testAddRecipe() throws Exception {

            ObjectNode json = mapper.createObjectNode();
            json.put("title", "Cake");
            json.put("description", "Delicious cake");
            // Change ingredients to a single String
            json.put("ingredients", "1 cup of flour, 1 cup of sugar, 3 eggs");
            json.put("instructions", "Mix and bake");
            String jsonString = mapper.writeValueAsString(json);
            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonString))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Cake"))
                    .andExpect(jsonPath("$.description").value("Delicious cake"))
                    .andExpect(jsonPath("$.ingredients").value("1 cup of flour, 1 cup of sugar, 3 eggs"))
                    .andExpect(jsonPath("$.instructions").value("Mix and bake"))
                    .andExpect(jsonPath("$.id").isNumber());
        }

        @ParameterizedTest
        @CsvSource({
                "'Chocolate Cake','Rich chocolate cake','2 cups flour;1 cup cocoa;4 eggs','Bake at 350F for 30 min'",
                "'Pasta Salad','Fresh pasta salad','200g pasta;100g tomatoes;50g olives','Mix all ingredients'",
                "'Pancakes','Fluffy pancakes','1 cup flour;2 eggs;1 cup milk','Cook on skillet until golden'"
        })
        void parameterizedAddRecipeTest(String title, String description, String ingredients, String instructions) throws Exception {
            throw new UnsupportedOperationException("parameterizedAddRecipeTest");
        }
    }

    // Internal class for delete and get tests
    @Nested
    class DeleteAndGetTests {
        private List<Integer> recipeIds;

        @BeforeEach
        void createRecipes() throws Exception {
            recipeIds = new ArrayList<>();
            String[] recipes = {
                    "{\"title\":\"Pie\",\"description\":\"Apple pie\",\"ingredients\":\"Apples, Flour, Sugar\",\"instructions\":\"Mix and bake\"}",
                    "{\"title\":\"Soup\",\"description\":\"Tomato soup\",\"ingredients\":\"Tomatoes, Water, Salt\",\"instructions\":\"Boil and blend\"}"
            };
            for (String json : recipes) {
                String response = mockMvc.perform(post("/api/recipes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
                int id = mapper.readTree(response).get("id").asInt();
                recipeIds.add(id);
            }
        }

        @Test
        void testGetAllRecipes() throws Exception {
            mockMvc.perform(get("/api/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Pie"))
                    .andExpect(jsonPath("$[1].title").value("Soup"));
        }

        @Test
        void testGetRecipe() throws Exception {
            int id = recipeIds.get(0);
            mockMvc.perform(get("/api/recipes/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Pie"));
        }

        @Test
        void testDeleteRecipe() throws Exception {
            int id = recipeIds.get(0);

            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .delete("/api/recipes/" + id)
                    )
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/recipes/" + id))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testPutRecipe() throws Exception {
            int id = recipeIds.get(0);

            ObjectNode json = mapper.createObjectNode();
            json.put("title", "Updated Pie");
            json.put("description", "Updated description");
            json.put("ingredients", "Apples, Flour, Sugar");
            json.put("instructions", "Bake at 375F");

            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .put("/api/recipes/" + id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(json))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Pie"))
                    .andExpect(jsonPath("$.description").value("Updated description"))
                    .andExpect(jsonPath("$.ingredients").value("Apples, Flour, Sugar"))
                    .andExpect(jsonPath("$.instructions").value("Bake at 375F"));
        }

        @Test
        void testPatchRecipe() throws Exception {
            int id = recipeIds.get(0);

            ObjectNode json = mapper.createObjectNode();
            json.put("description", "Updated via PATCH");

            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .patch("/api/recipes/" + id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(json))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Updated via PATCH"))
                    .andExpect(jsonPath("$.title").value("Pie"));
        }
    @Nested
    class NonExistingRecipeTests {

        @Test
        void testGetNonExistingRecipe() throws Exception {
            mockMvc.perform(get("/api/recipes/9999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testPutNonExistingRecipe() throws Exception {
            ObjectNode json = mapper.createObjectNode();
            json.put("title", "Non-existent recipe");
            json.put("description", "This recipe does not exist");
            json.put("ingredients", "None");
            json.put("instructions", "N/A");

            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .put("/api/recipes/9999")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(json))
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        void testPatchNonExistingRecipe() throws Exception {
            ObjectNode json = mapper.createObjectNode();
            json.put("description", "This recipe does not exist");

            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .patch("/api/recipes/9999")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(json))
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        void testDeleteNonExistingRecipe() throws Exception {
            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .delete("/api/recipes/9999")
                    )
                    .andExpect(status().isNotFound());
        }
    }



}