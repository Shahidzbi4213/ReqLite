package com.learn.reqlite.ui.home

object PostmanSampleCollections {
    val TEBYAN_DUA_API_JSON = """
{
	"info": {
		"_postman_id": "45e8e679-6991-4134-b143-4da662d5e988",
		"name": "Tebyan Dua Api",
		"schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
	},
	"item": [
		{
			"name": "Categories",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/dua-categories",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"dua-categories"
					]
				}
			},
			"response": []
		},
		{
			"name": "Category By ID",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/dua-categories/4",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"dua-categories",
						"4"
					]
				}
			},
			"response": []
		},
		{
			"name": "Content",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/dua-content?page=1&category_id=1",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"dua-content"
					],
					"query": [
						{
							"key": "page",
							"value": "1"
						},
						{
							"key": "parent_id",
							"value": null,
							"description": "parent category id",
							"disabled": true
						},
						{
							"key": "category_id",
							"value": "1"
						}
					]
				}
			},
			"response": []
		},
		{
			"name": "Content By ID",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/dua-content/249",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"dua-content",
						"249"
					]
				}
			},
			"response": []
		},
		{
			"name": "Most Viewed Content",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/dua-content-viewed",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"dua-content-viewed"
					]
				}
			},
			"response": []
		},
		{
			"name": "Search",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/dua-content-search?limit&category_id&hadith_ids&q=test",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"dua-content-search"
					],
					"query": [
						{
							"key": "limit",
							"value": null
						},
						{
							"key": "category_id",
							"value": null
						},
						{
							"key": "hadith_ids",
							"value": null,
							"description": "comma separated ids"
						},
						{
							"key": "q",
							"value": "test",
							"description": "search string"
						}
					]
				}
			},
			"response": []
		},
		{
			"name": "Content Viewed",
			"request": {
				"method": "PUT",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/dua-content/249/view",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"dua-content",
						"249",
						"view"
					]
				}
			},
			"response": []
		},
		{
			"name": "Books",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/dua-books?lang=ar",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"dua-books"
					],
					"query": [
						{
							"key": "lang",
							"value": "ar"
						}
					]
				}
			},
			"response": []
		},
		{
			"name": "Languages",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/languages",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"languages"
					]
				}
			},
			"response": []
		}
	],
	"variable": [
		{
			"key": "url",
			"value": "https://api.tebyan.com",
			"type": "string"
		}
	]
}
""".trimIndent()

    val TEBYAN_HADITH_APIS_JSON = """
{
	"info": {
		"_postman_id": "6bf4fbc7-7d9c-4f49-931f-20aefb3bc1c7",
		"name": "Tebyan Hadith APIs",
		"schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
	},
	"item": [
		{
			"name": "Categories",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/categories",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"categories"
					]
				}
			},
			"response": []
		},
		{
			"name": "Hadith List",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/hadith?sort_field=views&sort_direction=desc&limit=10&category_id&page",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"hadith"
					],
					"query": [
						{
							"key": "sort_field",
							"value": "views",
							"description": "number, views"
						},
						{
							"key": "sort_direction",
							"value": "desc"
						},
						{
							"key": "limit",
							"value": "10"
						},
						{
							"key": "category_id",
							"value": null
						},
						{
							"key": "page",
							"value": null
						}
					]
				}
			},
			"response": []
		},
		{
			"name": "Category By Id",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/categories/1",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"categories",
						"1"
					]
				}
			},
			"response": []
		},
		{
			"name": "Hadith By ID",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/hadith/3772",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"hadith",
						"3772"
					]
				}
			},
			"response": []
		},
		{
			"name": "Hadith Viewed",
			"request": {
				"method": "PUT",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/hadith/3772/view",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"hadith",
						"3772",
						"view"
					]
				}
			},
			"response": []
		},
		{
			"name": "Search",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/hadith?q=allah",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"hadith"
					],
					"query": [
						{
							"key": "q",
							"value": "allah"
						}
					]
				}
			},
			"response": []
		},
		{
			"name": "Books",
			"request": {
				"method": "GET",
				"header": [],
				"url": {
					"raw": "{{url}}/v1/hadith/books?lang=en",
					"host": [
						"{{url}}"
					],
					"path": [
						"v1",
						"hadith",
						"books"
					],
					"query": [
						{
							"key": "lang",
							"value": "en"
						}
					]
				}
			},
			"response": []
		}
	]
}
""".trimIndent()
}
