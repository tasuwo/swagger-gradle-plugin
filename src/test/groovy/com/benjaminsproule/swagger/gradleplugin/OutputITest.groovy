package com.benjaminsproule.swagger.gradleplugin

import groovy.json.JsonSlurper
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class OutputITest extends AbstractPluginITest {

    def 'Produces Swagger documentation with JAX-RS'() {
        given:
        def expectedSwaggerDirectory = "${testProjectOutputDir}/swaggerui-" + UUID.randomUUID()
        buildFile << """
            plugins {
                id 'java'
                id 'groovy'
                id 'com.benjaminsproule.swagger'
            }
            swagger {
                apiSource {
                    ${basicApiSourceClosure()}
                    swaggerDirectory = '${expectedSwaggerDirectory}'
                    ${testSpecificConfig}
                }
            }
        """

        when:
        def result = runPluginTask()

        then:
        result.task(":${GenerateSwaggerDocsTask.TASK_NAME}").outcome == SUCCESS

        assertSwaggerJson("${expectedSwaggerDirectory}/swagger.json", 'string')

        where:
        testSpecificConfig << [
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.groovy']
            """,
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.java']
            """,
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.kotlin']
            """
        ]
    }

    void 'Produces Swagger documentation with Spring MVC'() {
        given:
        def expectedSwaggerDirectory = "${testProjectOutputDir}/swaggerui-" + UUID.randomUUID()
        buildFile << """
            plugins {
                id 'java'
                id 'groovy'
                id 'com.benjaminsproule.swagger'
            }
            swagger {
                apiSource {
                    ${basicApiSourceClosure()}
                    swaggerDirectory = '${expectedSwaggerDirectory}'
                    ${testSpecificConfig}
                }
            }
        """

        when:
        def result = runPluginTask()

        then:
        result.task(":${GenerateSwaggerDocsTask.TASK_NAME}").outcome == SUCCESS

        assertSwaggerJson("${expectedSwaggerDirectory}/swagger.json", 'string')

        where:
        testSpecificConfig << [
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.groovy']
                springmvc = true
            """,
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.java']
                springmvc = true
            """,
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.kotlin']
                springmvc = true
            """
        ]
    }

    def 'Produces Swagger documentation with model substitution'() {
        given:
        def expectedSwaggerDirectory = "${testProjectOutputDir}/swaggerui-" + UUID.randomUUID()
        buildFile << """
            plugins {
                id 'java'
                id 'groovy'
                id 'com.benjaminsproule.swagger'
            }
            swagger {
                apiSource {
                    ${basicApiSourceClosure()}
                    swaggerDirectory = '${expectedSwaggerDirectory}'
                    ${testSpecificConfig}
                }
            }
        """

        when:
        def result = runPluginTask()

        then:
        result.task(":${GenerateSwaggerDocsTask.TASK_NAME}").outcome == SUCCESS

        assertSwaggerJson("${expectedSwaggerDirectory}/swagger.json", 'integer')

        where:
        testSpecificConfig << [
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.groovy']
                modelSubstitute = 'model-substitution'
            """,
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.java']
                modelSubstitute = 'model-substitution'
            """,
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.kotlin']
                modelSubstitute = 'model-substitution'
            """
        ]
    }

    def 'Produce Swagger documentation in multiple formats'() {
        given:
        def expectedSwaggerDirectory = "${testProjectOutputDir}/swaggerui-" + UUID.randomUUID()
        buildFile << """
            plugins {
                id 'java'
                id 'groovy'
                id 'com.benjaminsproule.swagger'
            }
            swagger {
                apiSource {
                    ${basicApiSourceClosure()}
                    swaggerDirectory = '${expectedSwaggerDirectory}'
                    ${testSpecificConfig}
                }
            }
        """

        when:
        def result = runPluginTask()

        then:
        result.task(":${GenerateSwaggerDocsTask.TASK_NAME}").outcome == SUCCESS

        assertSwaggerJson("${expectedSwaggerDirectory}/swagger.json")
        assertSwaggerYaml("${expectedSwaggerDirectory}/swagger.yaml")

        where:
        testSpecificConfig << [
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.groovy']
                outputFormats = ['json', 'yaml']
            """,
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.java']
                outputFormats = ['json', 'yaml']
            """,
            """
                locations = ['com.benjaminsproule.swagger.gradleplugin.test.kotlin']
                outputFormats = ['json', 'yaml']
            """
        ]
    }

    private static String basicApiSourceClosure() {
        """
        schemes = ['http']
        info {
            title = 'test'
            version = '1'
            license {
                name = 'Apache 2.0'
            }
            contact {
                name = 'Joe Blogs'
            }
        }
        host = 'localhost:8080'
        basePath = '/'
        securityDefinition {
            name = 'MyBasicAuth'
            type = 'basic'
        }
    """
    }

    private static void assertSwaggerJson(String swaggerJsonFilePath, String type = 'string') {
        def swaggerJsonFile = new File(swaggerJsonFilePath)
        assert Files.exists(swaggerJsonFile.toPath())
        assertSwaggerDocument(new JsonSlurper().parse(swaggerJsonFile, 'UTF-8'), 'json', type)
    }

    private static void assertSwaggerYaml(String swaggerYamlFilePath, String type = 'string') {
        def swaggerYamlFile = new File(swaggerYamlFilePath)
        assert Files.exists(swaggerYamlFile.toPath())
        assertSwaggerDocument(new Yaml().load(swaggerYamlFile.getText('UTF-8')), 'yaml', type)
    }

    private static void assertSwaggerDocument(def producedSwaggerDocument, String format, String type) {
        assert producedSwaggerDocument.swagger == '2.0'
        assert producedSwaggerDocument.host == 'localhost:8080'
        assert producedSwaggerDocument.basePath == '/'

        def info = producedSwaggerDocument.info
        assert info
        assert info.version == '1'
        assert info.title == 'test'
        assert info.contact.name == 'Joe Blogs'
        assert info.license.name == 'Apache 2.0'

        def tags = producedSwaggerDocument.tags
        assert tags
        assert tags.size() == 1
        assert tags.get(0).name == 'Test'

        def schemes = producedSwaggerDocument.schemes
        assert schemes
        assert schemes.size() == 1
        assert schemes.get(0) == 'http'

        def paths = producedSwaggerDocument.paths
        assert paths
        assert paths.size() == 26
        assertPaths(paths, format, type, 'withannotation')
        assertPaths(paths, format, type, 'withoutannotation')

        def securityDefinitions = producedSwaggerDocument.securityDefinitions
        assert securityDefinitions
        assert securityDefinitions.size() == 1
        assert securityDefinitions.MyBasicAuth.type == 'basic'

        def definitions = producedSwaggerDocument.definitions
        assert definitions
        assert definitions.size() == 3
        assert definitions.RequestModel.type == 'object'
        assert definitions.RequestModel.properties.size() == 2
        assert definitions.RequestModel.properties.name.type == type
        assert definitions.RequestModel.properties.value.type == type
        assert definitions.ResponseModel.type == 'object'
        assert definitions.ResponseModel.properties.size() == 1
        assert definitions.ResponseModel.properties.name.type == type
        assert definitions.SubResponseModel.type == 'object'
        assert definitions.SubResponseModel.properties.size() == 2
        assert definitions.SubResponseModel.properties.name.type == type
        assert definitions.SubResponseModel.properties.value.type == type
    }

    private static void assertPaths(paths, String format, String type, String path) {
        def ok = format == 'json' ? '200' : 200

        assert paths."/root/${path}/basic".get.tags == ['Test']
        assert paths."/root/${path}/basic".get.summary == 'A basic operation'
        assert paths."/root/${path}/basic".get.description == 'Test resource'
        assert paths."/root/${path}/basic".get.operationId == 'basic'
        assert paths."/root/${path}/basic".get.produces == null
        assert paths."/root/${path}/basic".get.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/basic".get.responses.get(ok).schema.type == type
        assert paths."/root/${path}/basic".get.security.basic

        assert paths."/root/${path}/default".get.tags == ['Test']
        assert paths."/root/${path}/default".get.summary == 'A default operation'
        assert paths."/root/${path}/default".get.description == 'Test resource'
        assert paths."/root/${path}/default".get.operationId == 'defaultResponse'
        assert paths."/root/${path}/default".get.produces == null
        if (paths."/root/${path}/default".get.responses.default) {
            assert paths."/root/${path}/default".get.responses.default.description == 'successful operation'
        } else if (paths."/root/${path}/default".get.responses.get(ok)) {
            assert paths."/root/${path}/default".get.responses.get(ok).description == 'successful operation'
        } else {
            assert false: "No response found for /root/${path}/default"
        }
        assert paths."/root/${path}/default".get.security.basic

        assert paths."/root/${path}/generics".post.tags == ['Test']
        assert paths."/root/${path}/generics".post.summary == 'A generics operation'
        assert paths."/root/${path}/generics".post.description == 'Test resource'
        assert paths."/root/${path}/generics".post.operationId == 'generics'
        assert paths."/root/${path}/generics".post.produces == null
        assert paths."/root/${path}/generics".post.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/generics".post.responses.get(ok).schema.type == 'array'
        assert paths."/root/${path}/generics".post.responses.get(ok).schema.items.type == type
        assert paths."/root/${path}/generics".post.security.basic
        assert paths."/root/${path}/generics".post.parameters[0].schema.type == 'array'
        assert paths."/root/${path}/generics".post.parameters[0].schema.items.'$ref' == '#/definitions/RequestModel'

        assert paths."/root/${path}/datatype".post.tags == ['Test']
        assert paths."/root/${path}/datatype".post.summary == 'Consumes and Produces operation'
        assert paths."/root/${path}/datatype".post.description == 'Test resource'
        assert paths."/root/${path}/datatype".post.operationId == 'dataType'
        assert paths."/root/${path}/datatype".post.produces == ['application/json']
        if (paths."/root/${path}/datatype".post.responses.default) {
            assert paths."/root/${path}/datatype".post.responses.default.description == 'successful operation'
        } else if (paths."/root/${path}/datatype".post.responses.get(ok)) {
            assert paths."/root/${path}/datatype".post.responses.get(ok).description == 'successful operation'
        } else {
            assert false: "No response found for /root/${path}/datatype"
        }
        assert paths."/root/${path}/datatype".post.security.basic
        assert paths."/root/${path}/datatype".post.parameters[0].name == 'body'
        assert paths."/root/${path}/datatype".post.parameters[0].schema.'$ref' == '#/definitions/RequestModel'

        assert paths."/root/${path}/response".post.tags == ['Test']
        assert paths."/root/${path}/response".post.summary == 'A response operation'
        assert paths."/root/${path}/response".post.description == 'Test resource'
        assert paths."/root/${path}/response".post.operationId == 'response'
        assert paths."/root/${path}/response".post.produces == null
        assert paths."/root/${path}/response".post.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/response".post.responses.get(ok).schema.type == null
        assert paths."/root/${path}/response".post.responses.get(ok).schema.'$ref' == '#/definitions/ResponseModel'
        assert paths."/root/${path}/response".post.security.basic

        assert paths."/root/${path}/responseContainer".post.tags == ['Test']
        assert paths."/root/${path}/responseContainer".post.summary == 'A response container operation'
        assert paths."/root/${path}/responseContainer".post.description == 'Test resource'
        assert paths."/root/${path}/responseContainer".post.operationId == 'responseContainer'
        assert paths."/root/${path}/responseContainer".post.produces == null
        assert paths."/root/${path}/responseContainer".post.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/responseContainer".post.responses.get(ok).schema.type == 'array'
        assert paths."/root/${path}/responseContainer".post.responses.get(ok).schema.items.'$ref' == '#/definitions/ResponseModel'
        assert paths."/root/${path}/responseContainer".post.security.basic

        assert paths."/root/${path}/extended".get.tags == ['Test']
        assert paths."/root/${path}/extended".get.summary == 'An extended operation'
        assert paths."/root/${path}/extended".get.description == 'Test resource'
        assert paths."/root/${path}/extended".get.operationId == 'extended'
        assert paths."/root/${path}/extended".get.produces == null
        assert paths."/root/${path}/extended".get.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/extended".get.responses.get(ok).schema.type == null
        assert paths."/root/${path}/extended".get.responses.get(ok).schema.'$ref' == '#/definitions/SubResponseModel'
        assert paths."/root/${path}/extended".get.security.basic

        assert paths."/root/${path}/deprecated".get.tags == ['Test']
        assert paths."/root/${path}/deprecated".get.summary == 'A deprecated operation'
        assert paths."/root/${path}/deprecated".get.description == 'Test resource'
        assert paths."/root/${path}/deprecated".get.operationId == 'deprecated'
        assert paths."/root/${path}/deprecated".get.produces == null
        assert paths."/root/${path}/deprecated".get.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/deprecated".get.responses.get(ok).schema.type == type
        assert paths."/root/${path}/deprecated".get.security.basic

        assert paths."/root/${path}/auth".get.tags == ['Test']
        assert paths."/root/${path}/auth".get.summary == 'An auth operation'
        assert paths."/root/${path}/auth".get.description == 'Test resource'
        assert paths."/root/${path}/auth".get.operationId == 'withAuth'
        assert paths."/root/${path}/auth".get.produces == null
        assert paths."/root/${path}/auth".get.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/auth".get.responses.get(ok).schema.type == type
        assert paths."/root/${path}/auth".get.security.basic

        assert paths."/root/${path}/model".get.tags == ['Test']
        assert paths."/root/${path}/model".get.summary == 'A model operation'
        assert paths."/root/${path}/model".get.description == 'Test resource'
        assert paths."/root/${path}/model".get.operationId == 'model'
        assert paths."/root/${path}/model".get.produces == null
        assert paths."/root/${path}/model".get.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/model".get.responses.get(ok).schema.type == type
        assert paths."/root/${path}/model".get.security.basic

        assert paths."/root/${path}/overriden".get.tags == ['Test']
        assert paths."/root/${path}/overriden".get.summary == 'An overriden operation description'
        assert paths."/root/${path}/overriden".get.description == 'Test resource'
        assert paths."/root/${path}/overriden".get.operationId == 'overriden'
        assert paths."/root/${path}/overriden".get.produces == null
        assert paths."/root/${path}/overriden".get.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/overriden".get.responses.get(ok).schema.type == type
        assert paths."/root/${path}/overriden".get.security.basic

        assert paths."/root/${path}/overridenWithoutDescription".get.tags == ['Test']
        assert paths."/root/${path}/overridenWithoutDescription".get.summary == 'An overriden operation'
        assert paths."/root/${path}/overridenWithoutDescription".get.description == 'Test resource'
        assert paths."/root/${path}/overridenWithoutDescription".get.operationId == 'overridenWithoutDescription'
        assert paths."/root/${path}/overridenWithoutDescription".get.produces == null
        assert paths."/root/${path}/overridenWithoutDescription".get.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/overridenWithoutDescription".get.responses.get(ok).schema.type == type
        assert paths."/root/${path}/overridenWithoutDescription".get.security.basic

        assert paths."/root/${path}/hidden" == null

        assert paths."/root/${path}/multipleParameters/{parameter1}".get.tags == ['Test']
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.summary == 'A multiple parameters operation'
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.description == 'Test resource'
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.operationId == 'multipleParameters'
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.produces == null
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.responses.get(ok).description == 'successful operation'
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.responses.get(ok).schema.type == type
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.security.basic
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.parameters[0].name == 'parameter1'
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.parameters[0].type == 'number'
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.parameters[1].name == 'parameter2'
        assert paths."/root/${path}/multipleParameters/{parameter1}".get.parameters[1].type == 'boolean'
    }
}