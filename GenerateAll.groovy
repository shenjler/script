import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasObject
import com.intellij.debugger.ui.overhead.OverheadView
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.impl.ui.NotificationsUtil

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectUtil
import com.intellij.openapi.project.impl.ProjectManagerImpl
import com.intellij.util.containers.JBIterable
import com.sun.org.apache.bcel.internal.generic.DASTORE
import org.apache.commons.collections.map.HashedMap
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.text.StrSubstitutor
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.jetbrains.ide.script.IdeScriptEngineManager
import org.jetbrains.ide.script.Jsr223IdeScriptEngineManagerImpl
import com.intellij.database.extensions.Logger

import com.intellij.database.extensions.SchemaScriptBindings
import com.intellij.database.extensions.*

import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
//import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.commons.lang3.StringUtils



//全局处理对象
class GlobalHanlder{

    public static Logger logger;
    public static Project project;
    public static JBIterable<DasObject> selection;
    public static Clipboard clipboard;
    public static Files files;
    public static List<File> scripts;

    StringBuilder enSb = new StringBuilder();
    StringBuilder zhSb = new StringBuilder();
    StringBuilder defaultSb = new StringBuilder();
    LocalStringBuilder apiSb = new LocalStringBuilder();
    LocalStringBuilder postmanApiSb = new LocalStringBuilder();
    LocalStringBuilder errorSb = new LocalStringBuilder();



    public static Map<String,Object> config = new HashMap<String,Object>();

    static {

        config.put("entity",false)
        config.put("repository",false)
        config.put("service",false)
        config.put("serviceTest",true)
        config.put("controller",false)
        config.put("controllerTest",false)
        config.put("VueApi",true)
        config.put("VueView",true)
        config.put("impSerializable",true)
        config.put("extendBaseEntity",false)
        config.put("impSerializable",true)
        config.put("extendBaseService",true)
        config.put("prefix","Epf")
        config.put("company","PCCW")
        config.put("author","shenjl")
//        config.put("author","")
        config.put("baseEntityProperties", ["id", "createDate", "lastModifiedDate", "version"]);
        config.put("commonProp",['employeeId','enabled','createStaff','createDate','modifyDate','modifyStaff'])
        config.put("showProp",['remark'])
        config.put("suffix",
                ["Entity":".java",
                "Query":"Query.java",
                "Vo":"Vo.java",
                "Service":"Service.java",
                "ServiceImpl":"Service.java",
                "ServiceImplTest":"ServiceTest.java",
                "Controller":"Controller.java",
                "ControllerTest":"ControllerTest.java",
                "Api":"api.json",
                "VueApi":".js",
                "VueView":".vue",
                "Repository":"Repository.java"]);


    }
    Map globalMap = new HashMap();

    public GlobalHanlder(Logger logger, Project project, JBIterable selection,
                Clipboard clipboard, Files files, List<File> scripts){
        this.logger = logger;
        this.project = project;
        this.selection = selection;
        this.clipboard = clipboard;
        this.files = files;
        this.scripts = scripts;
    }
    // idea.log
    public void print(Object content){
        logger.print(content)
    }

    // event log
    public static void log(String content){
        String title= "LOG ";
        log(title,content);
    }

    public static void log(String title, String content){
        Notification nofi = new Notification("groupID",title,content, NotificationType.ERROR);
        nofi.notify(project);
    }

    public void generate(){
        files.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
            selection.filter {
                it instanceof DasTable && it.getKind() == ObjectKind.TABLE
            }.each {
                buildRelation(it)
            }.each {
                generate(it, dir)
                enSb.append("\r\n")
                zhSb.append("\r\n")
                defaultSb.append("\r\n")
                generateApi(it);

            }
            saveMessageProperties(dir)
            saveApi(dir)
        }
    }
    public void generateApiHead(){
        LocalStringBuilder lsb = new LocalStringBuilder();
        LocalStringBuilder postLsb = new LocalStringBuilder();
        lsb.append "{";
        lsb.appendLine "\t\"baseUri\" : \"/cmdb/epf\",";
        lsb.appendLine "\t\"api\" : { ";
        apiSb.insertAhead lsb.toString();


        postLsb.append "{";
        postLsb.appendLine "	\"info\": {";
        postLsb.appendLine "		\"name\": \"DHQOneOss\",";
        postLsb.appendLine "		\"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"";
        postLsb.appendLine "	},";
        postLsb.appendLine "	\"item\": [";
        postLsb.appendLine "		{";
        postLsb.appendLine "			\"name\": \"epf\",";
        postLsb.appendLine "			\"item\": [";
        postmanApiSb.insertAhead postLsb.toString();
    }
    public void generateApiFoot(){
        apiSb.delete(apiSb.length()-3,apiSb.length())
        apiSb.appendLine "\t}";
        apiSb.appendLine "}";


        postmanApiSb.delete(postmanApiSb.length()-1,postmanApiSb.length())
        postmanApiSb.appendLine"			]";
        postmanApiSb.appendLine"		}";
        postmanApiSb.appendLine"	]";
        postmanApiSb.appendLine"}";
    }


    public String buildPropertiesJson(DasTable table){
        Set vals = new HashSet();
        LocalStringBuilder propertiesJson = new LocalStringBuilder();

        propertiesJson.append "{"
        def fields = BaseUtil.calcFields(table)
        fields.each{
            if(it.name == "id" ){
                return ;
            }

            def key = "\\\"${it.name}\\\":";
            def type = it.get("type")
            def name = StringUtils.capitalize(it.name);
            def val = ""

            if(BaseUtil.isFk(it)){
                if(!it.isNotNull){
                    return
                }
                def prop = BaseUtil.getFkType(it,true,false)
                key = "\\\"${prop}\\\":";
                val = "{\\\"id\\\":1}"
            }else{
                if(type == "Integer"){
                    val = RandomUtils.nextInt(10,30)
                }else if(type == "String"){
                    val ="\\\""+name + "-" + RandomStringUtils.randomAlphabetic(5)+"\\\""
                }else if(type == "Double"){
                    val = String.format("%.2d", RandomUtils.nextDouble(5,10));
                }else if(type == "Float"){
                    val = String.format("%.2f",RandomUtils.nextFloat(3,5));
                }else if(type == "Date"){
                    val = new Date().getTime();
                }else if(type == "BigDecimal"){
                    String d = String.format("%.2f",RandomUtils.nextDouble(25,30))
                    val = d
                }
            }
            vals.add(key+val)
        }
        propertiesJson.append vals.join(",")
        propertiesJson.append "}"
        return propertiesJson.toString();
    }


    public void generateApi(DasTable table){
        def uncapEntityName = BaseUtil.javaName(table.getName(),false);
        apiSb.appendLine "\t\t\"${uncapEntityName}\" : {";
        apiSb.appendLine "\t\t\t\"add\"    : \"/${uncapEntityName}/add\",";
        apiSb.appendLine "\t\t\t\"get\"    : \"/${uncapEntityName}/\${id}\",";
        apiSb.appendLine "\t\t\t\"list\"   : \"/${uncapEntityName}/list\",";
        apiSb.appendLine "\t\t\t\"update\" : \"/${uncapEntityName}/\${id}\",";
        apiSb.appendLine "\t\t\t\"delete\" : \"/${uncapEntityName}/delete/\${id}\"";
        apiSb.appendLine "\t\t},";
        apiSb.appendLine ""


        String param = buildPropertiesJson(table)

        postmanApiSb.appendLine "\t\t\t\t{";
        postmanApiSb.appendLine "\t\t\t\t	\"name\": \"${uncapEntityName}\",";
        postmanApiSb.appendLine "\t\t\t\t	\"item\": [";
        postmanApiSb.appendLine "\t\t\t\t		{";
        postmanApiSb.appendLine "\t\t\t\t			\"name\": \"${uncapEntityName}/1 getOne\",";
        postmanApiSb.appendLine "\t\t\t\t			\"request\": {";
        postmanApiSb.appendLine "\t\t\t\t				\"method\": \"GET\",";
        postmanApiSb.appendLine "\t\t\t\t				\"header\": [],";
        postmanApiSb.appendLine "\t\t\t\t				\"url\": {";
        postmanApiSb.appendLine "\t\t\t\t					\"raw\": \"{{uri}}/{{server_path}}/cmdb/epf/${uncapEntityName}/1\",";
        postmanApiSb.appendLine "\t\t\t\t					\"host\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{uri}}\"";
        postmanApiSb.appendLine "\t\t\t\t					],";
        postmanApiSb.appendLine "\t\t\t\t					\"path\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{server_path}}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"cmdb\",";
        postmanApiSb.appendLine "\t\t\t\t						\"epf\",";
        postmanApiSb.appendLine "\t\t\t\t						\"${uncapEntityName}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"1\"";
        postmanApiSb.appendLine "\t\t\t\t					]";
        postmanApiSb.appendLine "\t\t\t\t				}";
        postmanApiSb.appendLine "\t\t\t\t			}";
        postmanApiSb.appendLine "\t\t\t\t		},";
        postmanApiSb.appendLine "\t\t\t\t		{";
        postmanApiSb.appendLine "\t\t\t\t			\"name\": \"${uncapEntityName}/add\",";
        postmanApiSb.appendLine "\t\t\t\t			\"request\": {";
        postmanApiSb.appendLine "\t\t\t\t				\"method\": \"POST\",";
        postmanApiSb.appendLine "\t\t\t\t				\"header\": [";
        postmanApiSb.appendLine "\t\t\t\t					{";
        postmanApiSb.appendLine "\t\t\t\t						\"key\": \"Content-Type\",";
        postmanApiSb.appendLine "\t\t\t\t						\"name\": \"Content-Type\",";
        postmanApiSb.appendLine "\t\t\t\t						\"value\": \"application/json\",";
        postmanApiSb.appendLine "\t\t\t\t						\"type\": \"text\"";
        postmanApiSb.appendLine "\t\t\t\t					}";
        postmanApiSb.appendLine "\t\t\t\t				],";
        postmanApiSb.appendLine "\t\t\t\t				\"body\": {";
        postmanApiSb.appendLine "\t\t\t\t					\"mode\": \"raw\",";
        postmanApiSb.appendLine "\t\t\t\t					\"raw\": \"${param}\"";
        postmanApiSb.appendLine "\t\t\t\t				},";
        postmanApiSb.appendLine "\t\t\t\t				\"url\": {";
        postmanApiSb.appendLine "\t\t\t\t					\"raw\": \"{{uri}}/{{server_path}}/cmdb/epf/${uncapEntityName}/add\",";
        postmanApiSb.appendLine "\t\t\t\t					\"host\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{uri}}\"";
        postmanApiSb.appendLine "\t\t\t\t					],";
        postmanApiSb.appendLine "\t\t\t\t					\"path\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{server_path}}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"cmdb\",";
        postmanApiSb.appendLine "\t\t\t\t						\"epf\",";
        postmanApiSb.appendLine "\t\t\t\t						\"${uncapEntityName}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"add\"";
        postmanApiSb.appendLine "\t\t\t\t					]";
        postmanApiSb.appendLine "\t\t\t\t				}";
        postmanApiSb.appendLine "\t\t\t\t			}";
        postmanApiSb.appendLine "\t\t\t\t		},";
        postmanApiSb.appendLine "\t\t\t\t		{";
        postmanApiSb.appendLine "\t\t\t\t			\"name\": \"${uncapEntityName}/update/2\",";
        postmanApiSb.appendLine "\t\t\t\t			\"request\": {";
        postmanApiSb.appendLine "\t\t\t\t				\"method\": \"PUT\",";
        postmanApiSb.appendLine "\t\t\t\t				\"header\": [";
        postmanApiSb.appendLine "\t\t\t\t					{";
        postmanApiSb.appendLine "\t\t\t\t						\"key\": \"Content-Type\",";
        postmanApiSb.appendLine "\t\t\t\t						\"name\": \"Content-Type\",";
        postmanApiSb.appendLine "\t\t\t\t						\"value\": \"application/json\",";
        postmanApiSb.appendLine "\t\t\t\t						\"type\": \"text\"";
        postmanApiSb.appendLine "\t\t\t\t					}";
        postmanApiSb.appendLine "\t\t\t\t				],";
        postmanApiSb.appendLine "\t\t\t\t				\"body\": {";
        postmanApiSb.appendLine "\t\t\t\t					\"mode\": \"raw\",";
        postmanApiSb.appendLine "\t\t\t\t					\"raw\": \"${param}\"";
        postmanApiSb.appendLine "\t\t\t\t				},";
        postmanApiSb.appendLine "\t\t\t\t				\"url\": {";
        postmanApiSb.appendLine "\t\t\t\t					\"raw\": \"{{uri}}/{{server_path}}/cmdb/epf/${uncapEntityName}/update/1\",";
        postmanApiSb.appendLine "\t\t\t\t					\"host\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{uri}}\"";
        postmanApiSb.appendLine "\t\t\t\t					],";
        postmanApiSb.appendLine "\t\t\t\t					\"path\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{server_path}}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"cmdb\",";
        postmanApiSb.appendLine "\t\t\t\t						\"epf\",";
        postmanApiSb.appendLine "\t\t\t\t						\"${uncapEntityName}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"update\",";
        postmanApiSb.appendLine "\t\t\t\t						\"1\"";
        postmanApiSb.appendLine "\t\t\t\t					]";
        postmanApiSb.appendLine "\t\t\t\t				}";
        postmanApiSb.appendLine "\t\t\t\t			}";
        postmanApiSb.appendLine "\t\t\t\t		},";
        postmanApiSb.appendLine "\t\t\t\t		{";
        postmanApiSb.appendLine "\t\t\t\t			\"name\": \"${uncapEntityName}/delete\",";
        postmanApiSb.appendLine "\t\t\t\t			\"request\": {";
        postmanApiSb.appendLine "\t\t\t\t				\"method\": \"DELETE\",";
        postmanApiSb.appendLine "\t\t\t\t				\"header\": [";
        postmanApiSb.appendLine "\t\t\t\t					{";
        postmanApiSb.appendLine "\t\t\t\t						\"key\": \"Content-Type\",";
        postmanApiSb.appendLine "\t\t\t\t						\"name\": \"Content-Type\",";
        postmanApiSb.appendLine "\t\t\t\t						\"value\": \"application/json\",";
        postmanApiSb.appendLine "\t\t\t\t						\"type\": \"text\"";
        postmanApiSb.appendLine "\t\t\t\t					}";
        postmanApiSb.appendLine "\t\t\t\t				],";
        postmanApiSb.appendLine "\t\t\t\t				\"body\": {";
        postmanApiSb.appendLine "\t\t\t\t					\"mode\": \"raw\",";
        postmanApiSb.appendLine "\t\t\t\t					\"raw\": \"\"";
        postmanApiSb.appendLine "\t\t\t\t				},";
        postmanApiSb.appendLine "\t\t\t\t				\"url\": {";
        postmanApiSb.appendLine "\t\t\t\t					\"raw\": \"{{uri}}/{{server_path}}/cmdb/epf/${uncapEntityName}/delete/3\",";
        postmanApiSb.appendLine "\t\t\t\t					\"host\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{uri}}\"";
        postmanApiSb.appendLine "\t\t\t\t					],";
        postmanApiSb.appendLine "\t\t\t\t					\"path\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{server_path}}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"cmdb\",";
        postmanApiSb.appendLine "\t\t\t\t						\"epf\",";
        postmanApiSb.appendLine "\t\t\t\t						\"${uncapEntityName}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"delete\",";
        postmanApiSb.appendLine "\t\t\t\t						\"3\"";
        postmanApiSb.appendLine "\t\t\t\t					]";
        postmanApiSb.appendLine "\t\t\t\t				}";
        postmanApiSb.appendLine "\t\t\t\t			}";
        postmanApiSb.appendLine "\t\t\t\t		},";
        postmanApiSb.appendLine "\t\t\t\t		{";
        postmanApiSb.appendLine "\t\t\t\t			\"name\": \"${uncapEntityName}/list\",";
        postmanApiSb.appendLine "\t\t\t\t			\"request\": {";
        postmanApiSb.appendLine "\t\t\t\t				\"method\": \"POST\",";
        postmanApiSb.appendLine "\t\t\t\t				\"header\": [";
        postmanApiSb.appendLine "\t\t\t\t					{";
        postmanApiSb.appendLine "\t\t\t\t						\"key\": \"Content-Type\",";
        postmanApiSb.appendLine "\t\t\t\t						\"name\": \"Content-Type\",";
        postmanApiSb.appendLine "\t\t\t\t						\"value\": \"application/json\",";
        postmanApiSb.appendLine "\t\t\t\t						\"type\": \"text\"";
        postmanApiSb.appendLine "\t\t\t\t					}";
        postmanApiSb.appendLine "\t\t\t\t				],";
        postmanApiSb.appendLine "\t\t\t\t				\"body\": {";
        postmanApiSb.appendLine "\t\t\t\t					\"mode\": \"raw\",";
        postmanApiSb.appendLine "\t\t\t\t					\"raw\": \"\"";
        postmanApiSb.appendLine "\t\t\t\t				},";
        postmanApiSb.appendLine "\t\t\t\t				\"url\": {";
        postmanApiSb.appendLine "\t\t\t\t					\"raw\": \"{{uri}}/{{server_path}}/cmdb/epf/${uncapEntityName}/list\",";
        postmanApiSb.appendLine "\t\t\t\t					\"host\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{uri}}\"";
        postmanApiSb.appendLine "\t\t\t\t					],";
        postmanApiSb.appendLine "\t\t\t\t					\"path\": [";
        postmanApiSb.appendLine "\t\t\t\t						\"{{server_path}}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"cmdb\",";
        postmanApiSb.appendLine "\t\t\t\t						\"epf\",";
        postmanApiSb.appendLine "\t\t\t\t						\"${uncapEntityName}\",";
        postmanApiSb.appendLine "\t\t\t\t						\"list\"";
        postmanApiSb.appendLine "\t\t\t\t					]";
        postmanApiSb.appendLine "\t\t\t\t				}";
        postmanApiSb.appendLine "\t\t\t\t			}";
        postmanApiSb.appendLine "\t\t\t\t		}";
        postmanApiSb.appendLine "\t\t\t\t	],";
        postmanApiSb.appendLine "\t\t\t\t	\"_postman_isSubFolder\": true";
        postmanApiSb.appendLine "\t\t\t\t},";
    }

    def saveApi(dir) {
        generateApiHead();
        generateApiFoot();
        def baseDir = StringUtils.substringBefore(dir.toString(), "\\java")
        def validDir = baseDir + "\\resources"

        def apiJson = new File(validDir,config.suffix.Api)
        FileUtils.writeStringToFile(apiJson,apiSb.toString(),"UTF-8")


        def postmanApiJson = new File(validDir,"postman_"+config.suffix.Api)
        FileUtils.writeStringToFile(postmanApiJson,postmanApiSb.toString(),"UTF-8")
    }

    def saveMessageProperties(dir){
        def baseDir = StringUtils.substringBefore(dir.toString(),"\\java")
        def validDir = baseDir + "\\resources\\i18n\\validation"

        def enFile = new File(validDir,"validationMessages_en_US.properties")
        def zhFile = new File(validDir,"validationMessages_zh_CN.properties")
        def defaultFile = new File(validDir,"validationMessages.properties")
        def errorFile = new File(validDir,"errorMsg.properties")

        FileUtils.writeStringToFile(enFile,enSb.toString(),"UTF-8")
        FileUtils.writeStringToFile(zhFile,defaultSb.toString(),"UTF-8")
        FileUtils.writeStringToFile(defaultFile,zhSb.toString(),"UTF-8")
        FileUtils.writeStringToFile(errorFile,errorSb.toString(),"UTF-8")

    }

    public void generate(DasTable table, File dir){

        EntityGenerator entityGenerator = new EntityGenerator(this);
        RepositoryGenerator repositoryGenerator = new RepositoryGenerator(this);
        ServiceGenerator serviceGenerator = new ServiceGenerator(this);
        ServiceImplGenerator serviceImplGenerator = new ServiceImplGenerator(this);
        ControllerGenerator controllerGenerator = new ControllerGenerator(this);
        ServiceTestGenerator serviceTestGenerator = new ServiceTestGenerator(this);
        ControllerTestGenerator controllerTestGenerator = new ControllerTestGenerator(this);
        VueApiGenerator vueApiGenerator = new VueApiGenerator(this);
        VueViewGenerator viewGenerator = new VueViewGenerator(this);

        entityGenerator.generate(table,dir)
        repositoryGenerator.generate(table,dir)
        serviceGenerator.generate(table,dir)
        serviceImplGenerator.generate(table,dir)
        controllerGenerator.generate(table,dir)
        serviceTestGenerator.generate(table,dir)
        controllerTestGenerator.generate(table,dir)
        vueApiGenerator.generate(table,dir)
        viewGenerator.generate(table,dir)
    }

    //查找一对多关系
    def buildRelation(DasTable table){
        def fields = BaseUtil.calcFields(table)
        fields.each() {
            if(BaseUtil.isFk(it)){
                //一对多，一端
                def fkType = BaseUtil.getFkType(it)

                //特殊处理Cable,
                fkType = BaseUtil.specailHandle(fkType)

                List<String> list = globalMap.get(fkType)
                if(list == null){
                    list = new ArrayList<String>()
                    globalMap.put(fkType,list)
                }
                def entityName = BaseUtil.javaName(it.table.getName(),true)
                list.add(entityName+"|"+it.colum)
            }
        }
    }

}



typeMapping = [
        (~/(?i)bool|boolean|tinyint/)     : "Boolean",
        (~/(?i)bigint/)                   : "Long",
        (~/int/)                          : "Integer",
        (~/(?i)decimal/)                  : "BigDecimal",
        (~/(?i)double|real/)              : "Double",
        (~/(?i)float/)                    : "Float",
        (~/(?i)datetime|timestamp/)       : "Date",
        (~/(?i)date/)                     : "java.sql.Date",
        (~/(?i)time/)                     : "java.sql.Time",
        (~/(?i)/)                         : "String"
]

//公共的函数抽取
BaseUtil.typeMapping = typeMapping

class BaseUtil{
    public static GlobalHanlder handler;
    public static String prefix = "Epf"
    public static Map typeMapping;
    //是否是外键
    public static Boolean isFk(field){
        return StringUtils.endsWith(field.colum,"_ID") &&
                StringUtils.containsIgnoreCase(field.comment,"FK")
    }

    def static getFkType(field){

        return getFkType(field,true,true)
    }

    def static getFkType(field,withPrefix,capitalize){

        if(!isFk(field)){
            return null
        }
//        handler.log(field.comment)
        def col = StringUtils.substringAfter(field.comment,'FK_')
        return javaName(col,capitalize)

//        def fieldName = StringUtils.replace(field.name,"Id","")
//        def name = capitalize ? StringUtils.capitalize(fieldName) : fieldName
//        return withPrefix ? prefix+name : name
    }

    //字段与表名不一致,特殊处理
    def static specailHandle(fkType){
        if(fkType == "EpfCable"){
            fkType = "EpfOpticalCable"
        }
        if(fkType == "EpfFiber"){
            fkType = "EpfOpticalFiber"
        }
        return  fkType
    }

    //字段 类型 与表名不一致,特殊处理
    def static specailTypeHandle(fkType){
//        if(fkType == "EpfOnuPort"){
//            fkType = "EpfPort"
//        }
        return  fkType
    }

    // 生成文件夹
    def static mkdirs(dirs) {
        dirs.forEach {
            def f = new File(it)
            if (!f.exists()) {
                f.mkdirs()
            }
        }
    }

    def static mkdir(dir) {
        def f = new File(dir)
        if (!f.exists()) {
            f.mkdirs()
        }

    }


    def static clacBasePackage(dir) {
        dir.toString()
                .replaceAll("^.+\\\\src\\\\(main|test)\\\\java\\\\", "")
                .replaceAll("\\\\", ".")
    }

    /**
     * 替换
     * @param source 源内容
     * @param parameter 占位符参数
     * @param prefix 占位符前缀 例如:${
     * @param suffix 占位符后缀 例如:}
     * @param enableSubstitutionInVariables 是否在变量名称中进行替换 例如:${system-${版本}}
     *
     * 转义符默认为'$'。如果这个字符放在一个变量引用之前，这个引用将被忽略，不会被替换 如$${a}将直接输出${a}
     * @return
     */
    def static String replace(String source,Map<String, Object> parameter) {
        //StrSubstitutor不是线程安全的类
        String prefix = "\$"+ "{";
        String suffix = "}";
        StrSubstitutor strSubstitutor = new StrSubstitutor(parameter,prefix, suffix);
        //是否在变量名称中进行替换
//        strSubstitutor.setEnableSubstitutionInVariables(enableSubstitutionInVariables);
        return strSubstitutor.replace(source);
    }

// 转换类型
    public static List<Map> calcFields(table) {
        JBIterable<? extends DasColumn> it = DasUtil.getColumns(table);
        Iterator i = it.iterator()
        ArrayList<Map> fields = new ArrayList<Map>()
        for(;i.hasNext();){
            DasColumn col = i.next();
            def spec = Case.LOWER.apply(col.getDataType().getSpecification())
            def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
            Map<String,Object> item = new HashMap<String,Object>();
            item.put("name", javaName(col.getName(), false));
            item.put("colum", col.getName());
            item.put("type", typeStr);
            item.put("len", col.getDataType().toString().replaceAll("[^\\d]", ""));
            item.put("default", col.getDefault());
            item.put("comment", col.getComment());
            def comment = col.getComment();
            if(StringUtils.startsWith(comment,"FK_") && StringUtils.endsWith(col.getName(),"_ID")){
                // comment: FK_EPF_OPTICAL_CABLE, name: CABLE_ID
                def type = StringUtils.substringAfter(comment,"FK_");
                def s = StringUtils.upperCase(handler.config.prefix+"_");
                def noPrefixType = StringUtils.replace(type, s ,"")
                item.put("fkType", javaName(noPrefixType,true))
                item.put("name", javaName(noPrefixType,false))
            }
            item.put("isNotNull", col.isNotNull());
            item.put("position", col.getPosition());
            item.put("table",col.getTable());
            fields.add(item)

        }
        return fields;

    }

    public static String javaName(str, capitalize) {
        def s = str.split(/(?<=[^\p{IsLetter}])/).collect { Case.LOWER.apply(it).capitalize() }
                .join("").replaceAll(/[^\p{javaJavaIdentifierPart}]/, "_").replaceAll(/_/, "")
        capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
    }

    public static void writeToFile(File file, String text) {
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        }
        FileUtils.writeStringToFile(file, text, "UTF-8")
    }

}

class BaseGenerator{
    public GlobalHanlder handler;
    protected Map<String, Object> params = new HashMap<String,Object>()
    protected String basePackage;
    protected LocalStringBuilder sb;
    protected String entityName;
    protected String uncapEntityName;
    protected String suffix;
    protected String desc;
    protected DasTable table;
    protected File dir;
    public String folder;
    protected String path;
    protected boolean hasOne = false;
    protected boolean hasMany = false;
    protected String tableComment;

    protected List<Map> fields;
    protected List<Map> fks = new ArrayList<Map>();
    protected Set<String> importSets = new HashSet<String>();
    protected Set<String> autowireSets = new HashSet<String>();




    public void setUpdateProperties(){
        fields.each {
            if(it.name == "id" || BaseUtil.isFk(it) ){
                return ;
            }
            def type = it.get("type")
            def name = StringUtils.capitalize(it.name);
            def val = ""
            if(type == "Integer"){
                val = RandomUtils.nextInt(10,30)
            }else if(type == "String"){
                val ="\""+name + "-" + RandomStringUtils.randomAlphabetic(5)+"\""
            }else if(type == "Double"){
                val = String.format("%.2fd", RandomUtils.nextDouble(5,10));
            }else if(type == "Float"){
                val = String.format("%.2ff",RandomUtils.nextFloat(3,5));
            }else if(type == "Date"){
                importSets.add "import java.util.Date;";
                val = "new Date()"
            }else if(type == "BigDecimal"){
                importSets.add "import java.math.BigDecimal;";
                String d = String.format("%.2f",RandomUtils.nextDouble(25,30))
                val = "new BigDecimal($d)"
            }
            sb.appendLine "\t    model.set${name}(${val});";
        }
    }



    public BaseGenerator(GlobalHanlder handler){
        this.handler =handler;
    }

    public void init(DasTable table, File dir){
        this.table = table
        this.dir = dir;
        this.basePackage = BaseUtil.clacBasePackage(dir)
        this.entityName = BaseUtil.javaName(table.getName(), true)
        this.uncapEntityName = StringUtils.uncapitalize(entityName);
        this.sb = new LocalStringBuilder();

        this.path = dir.toString() +"\\"+ folder;
        BaseUtil.mkdir(path)
        this.fields = BaseUtil.calcFields(table)

        def folderDesc = "";
        StringUtils.split(folder,"\\\\").each {
            folderDesc += StringUtils.capitalize(it)

        };

        this.desc = entityName+ " " + folderDesc
        this.tableComment = table.getComment()
        params.put("basePackage",basePackage)
        params.put("entityName",entityName)
        params.put("import","")
        params.put("handler.config.baseEntityPackage", handler.config.baseEntityPackage)


        for (def it in fields){
            if(BaseUtil.isFk(it)){
                fks.add(it)
                hasOne = true
            }
        }

        hasMany = handler.globalMap.containsKey(entityName);
    }

    void setParams(){
        if(!importSets.isEmpty()){
            params.put("import",importSets.join("\r\n"))
        }
        if(!autowireSets.isEmpty()){
            params.put("autowire",autowireSets.join("\r\n"))
        }
    }
    void writeToFile(){
        setParams();
        String text = BaseUtil.replace(sb.toString(),params);
        File file = null
        if(folder == "service"){
            file = new File(path,"I"+ entityName + suffix);
        }else if(folder == "vue" || folder == "view"){
            file = new File(path, uncapEntityName + suffix);
        }else {
            file = new File(path, entityName + suffix);
        }

        BaseUtil.writeToFile(file, text)
    }

    public appendComment(){
        def today = DateFormatUtils.format(new Date(),"yyyy-MM-dd")
        sb.appendLine "/**"
        sb.appendLine " * <p>Title:</p>"
        sb.appendLine " * <p>Description: $desc  $tableComment</p>"
        sb.appendLine " * <p>Copyright: Copyright (c) 2019</p>"
        sb.appendLine " * <p>Company: ${handler.config.company}</p>"
        sb.appendLine " *"
        sb.appendLine " * @author ${handler.config.author}"
        sb.appendLine " * @version 1.0"
        sb.appendLine " * @date $today"
        sb.appendLine " */"
    }
}

class LocalStringBuilder {

    private StringBuilder sb;
    public LocalStringBuilder(){
        this.sb = new StringBuilder();
    }
    public append(String content){
        this.sb.append(content);
    }

    public appendLine(String content){
        this.sb.append("\r\n").append(content);
    }
    public insertAhead(String content){
        this.sb.insert(0,content);
    }

    public deleteChatAt(int index){
        this.sb.deleteCharAt(index);
    }
    public delete(int start, int end){
        this.sb.delete(start,end);
    }

    public int length(){
        return this.sb.length();
    }

    public String toString(){
        return sb.toString();
    }


}


class EntityGenerator extends BaseGenerator implements Generator{

    EntityGenerator(GlobalHanlder handler) {
        super(handler)
        this.folder = "domain";
        this.suffix = handler.config.suffix.Entity;
    }

    @Override
    List<Map> generate(DasTable table, File dir) {

        if(!handler.config.entity){
            //handler.log("entity not generate, pass ");
            return null;
        }
        init(table, dir);
        generateImport();
        generateJavaDoc()
        generateBody()
        writeToFile();
        return fields;
    }

    @Override
    public void generateBody() {
//        sb.appendLine "@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = \"id\")";
        sb.appendLine("@Entity");
        sb.appendLine("@Table(name = \"${StringUtils.upperCase(table.getName())}\")");
        sb.appendLine("public class ${entityName}");
        String baseEntity = handler.config.extendBaseEntity ? " extends BaseEntity" : "";
        sb.append(baseEntity);
        String impSerializable = handler.config.impSerializable ? " implements Serializable" : ""
        sb.append(impSerializable);
        sb.append(" {");

        if (handler.config.extendBaseEntity) {
            fields = fields.findAll { it ->
                !handler.config.baseEntityProperties.any { it1 -> it1 == it.name }
            }
        }
        fields.each() {
            generateProperty(sb, it, params)
        }
        addOneToManyProperties(sb, entityName, params)

        fields.each() {
            generateGetSetter(sb, it, params)
        }
        addOneToManySetGetter(sb, entityName, params)
        sb.appendLine("}");
    }

    @Override
    public void generateImport() {
        sb.appendLine("package ${basePackage}.domain;");
        sb.appendLine("");
        if (handler.config.extendBaseEntity) {
            sb.appendLine("import ${handler.config.baseEntityPackage};");
        }
        if (handler.config.impSerializable) {
            sb.appendLine("import java.io.Serializable;");
            sb.appendLine("");
        }
//        sb.appendLine("import com.fasterxml.jackson.annotation.JsonIgnore;");
//        sb.appendLine("import com.fasterxml.jackson.annotation.JsonFormat;");
//        sb.appendLine "import com.fasterxml.jackson.annotation.JsonIdentityInfo;";
//        sb.appendLine "import com.fasterxml.jackson.annotation.ObjectIdGenerators;";

        sb.appendLine("import javax.validation.constraints.*;");

        sb.appendLine("import javax.persistence.*;");
        sb.appendLine("import javax.validation.constraints.NotBlank;");
        sb.appendLine("import org.hibernate.validator.constraints.*;");

        sb.appendLine("import java.util.Date;");
        if (handler.globalMap.containsKey(entityName)) {
            sb.appendLine("import java.util.List;");
        }
        sb.appendLine("\${import}");
    }

    @Override
    void generateJavaDoc() {
        appendComment();
        sb.appendLine("")
    }

    def buildI18nMessage(message){
        buildI18nMessage(message,"")
    }
    def buildI18nMessage(message, len){
        def enUS = null
        def zhCN = null
        if(StringUtils.endsWith(message,".not.null")){
            def fieldName = StringUtils.substringBefore(message,".not.null")
            enUS = fieldName + " must be not null."
            zhCN = fieldName + "不能为空。"
        }else if(StringUtils.endsWith(message,".length.limit")){
            def fieldName = StringUtils.substringBefore(message,".length.limit")
            enUS = fieldName + " length more than "+ len +"."
            zhCN = fieldName + "长度超过${len}个字符。"
        }

        handler.enSb.append(message).append("=").append(enUS).append("\r\n")
        handler.zhSb.append(message).append("=").append(zhCN).append("\r\n")
        def zhUnicode = StringEscapeUtils.escapeJava(zhCN)
        handler.defaultSb.append(message).append("=").append(zhUnicode).append("\r\n")


    }

    def void generateProperty(LocalStringBuilder sb, Map field, Map param){



        sb.appendLine ""
        sb.appendLine "\t/**"
        sb.appendLine "\t * ${field.comment}"
        sb.appendLine "\t * default value: ${field.default}"
        sb.appendLine "\t */"

        // 默认表的第一个字段为主键
        if (field.position == 1) {
            sb.appendLine("\t@Id");
            sb.appendLine("\t@GeneratedValue(strategy = GenerationType.IDENTITY)");
        }
        def entityName = BaseUtil.javaName(field.table.getName(),false)
        def fk = false
        if(field.type == "String"){
            def limitMessage = entityName+"."+field.name+".length.limit"
            sb.appendLine("\t@Length(max=${field.len}, message = \"{$limitMessage}\")");
            buildI18nMessage(limitMessage,field.len)
            if(field.isNotNull){
                def nbMessage = entityName+"."+field.name+".not.null"
                buildI18nMessage(nbMessage)
                sb.appendLine("\t@NotBlank(message = \"{$nbMessage}\")");
            }
        }else if(field.type == "Integer" && field.position != 1){
            //外键
            if(BaseUtil.isFk(field)) {
//                sb.appendLine("\t@JsonIgnore");
                sb.appendLine("\t@ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE})");
                fk = true
            }else{
                if(field.isNotNull){
                    //普通Integer
                    def nnMessage = entityName +"."+ field.name + ".not.null"
                    buildI18nMessage(nnMessage)
                    sb.appendLine("\t@NotNull(message = \"{$nnMessage}\")");
                }
            }
        }else if(field.type == "BigDecimal"){
            importSets.add("import java.math.BigDecimal;")
        }else if(field.type == "Double"){
            importSets.add("import java.util.Double;")
        }else if(field.type == "Date"){
//            sb.appendLine "\t@JsonFormat(pattern=\"yyyy-MM-dd\")";
        }

        if(!fk){

            if(field.len==""){
                sb.appendLine("\t@Column(name = \"${field.colum}\", nullable = ${!field.isNotNull})");
            }else{
                sb.appendLine("\t@Column(name = \"${field.colum}\", nullable = ${!field.isNotNull}, length = ${field.len})");
            }

            sb.appendLine("\tprivate ${field.type} ${field.name};");
        }else{

            def fkType = BaseUtil.getFkType(field)


            //特殊处理Cable,
            fkType = BaseUtil.specailHandle(fkType)


//            //外键id
//            sb.appendLine("\tprivate ${field.type} ${field.name};");
//            sb.appendLine("");



            fkType = BaseUtil.specailTypeHandle(fkType)


            if(field.isNotNull){
                def fkMessage = entityName +"."+ field.name + ".not.null"
                buildI18nMessage(fkMessage)
                sb.appendLine("\t@NotNull(message = \"{$fkMessage}\")");
            }
            // model对象
            sb.appendLine("\t@JoinColumn(name = \"${field.colum}\")");
//            sb.appendLine("\tprivate ${fkType} ${BaseUtil.getFkType(field,true,false)};");
            sb.appendLine("\tprivate ${fkType} ${StringUtils.uncapitalize(fkType)};");

        }
    }


    def fkSetGetter(sb,field){
        def fkType = BaseUtil.getFkType(field)

        //特殊处理Cable,
        fkType = BaseUtil.specailHandle(fkType)
        fkType = BaseUtil.specailTypeHandle(fkType)


//        def fkTypeWithoutPrefix = BaseUtil.getFkType(field,true,true)
//        def fkTypeWithoutPrefixCap = StringUtils.capitalize(fkTypeWithoutPrefix)

        def fkTypeWithPrefixCap = fkType;
        def fkTypeWithPrefix = StringUtils.uncapitalize(fkTypeWithPrefixCap)


        //get
        sb.appendLine "\t"
        sb.appendLine "\tpublic ${fkType} get${fkTypeWithPrefixCap}() {"
        sb.appendLine "\t\treturn this.${fkTypeWithPrefix};"
        sb.appendLine "\t}"

        //set
        sb.appendLine "\t"
        sb.appendLine "\tpublic void set${fkTypeWithPrefixCap}(${fkType} ${fkTypeWithPrefix}) {"
        sb.appendLine "\t\tthis.${fkTypeWithPrefix} = ${fkTypeWithPrefix};"
        sb.appendLine "\t}"

//        //get
//        sb.appendLine "\t"
//        sb.appendLine "\tpublic ${fkType} get${fkTypeWithoutPrefixCap}() {"
//        sb.appendLine "\t\treturn this.${fkTypeWithoutPrefix};"
//        sb.appendLine "\t}"
//
//        //set
//        sb.appendLine "\t"
//        sb.appendLine "\tpublic void set${fkTypeWithoutPrefixCap}(${fkType} ${fkTypeWithoutPrefix}) {"
//        sb.appendLine "\t\tthis.${fkTypeWithoutPrefix} = ${fkTypeWithoutPrefix};"
//        sb.appendLine "\t}"
    }

    def void generateGetSetter(LocalStringBuilder sb, Map field, Map param){
        if(BaseUtil.isFk(field)){
            fkSetGetter(sb,field)
            return;
        }

        // get
        sb.appendLine "\t"
        sb.appendLine "\tpublic ${field.type} get${field.name.substring(0, 1).toUpperCase()}${field.name.substring(1)}() {"
        sb.appendLine "\t\treturn this.${field.name};"
        sb.appendLine "\t}"

        // set
        sb.appendLine "\t"
        sb.appendLine "\tpublic void set${field.name.substring(0, 1).toUpperCase()}${field.name.substring(1)}(${field.type} ${field.name}) {"
        sb.appendLine "\t\tthis.${field.name} = ${field.name};"
        sb.appendLine "\t}"
    }

    //一对多,一端加List<T>
    def void addOneToManyProperties(LocalStringBuilder sb, String entityName, Map param){
        handler.globalMap.each{key, values ->
            if(key == entityName){
                values.each(){ it ->
                    def fkType = StringUtils.substringBefore(it,"|")
                    def colum = StringUtils.substringAfter(it,"|")
                    def prefix = handler.config.prefix;
                    if(!StringUtils.startsWith(fkType,prefix)){
                        fkType =prefix+fkType
                    }
                    def fkName = StringUtils.uncapitalize(fkType)
//
                   sb.appendLine("");
//                   sb.appendLine("\t@JsonIgnore");
                   sb.appendLine("\t@OneToMany(cascade=CascadeType.PERSIST)");
                   sb.appendLine("\t@JoinColumn(name = \"${colum}\")");
                   sb.appendLine("\tprivate List<$fkType> $fkName;");
                }
            }
        }
    }

    //一对多,一端加set getter
    def void addOneToManySetGetter(LocalStringBuilder sb, String entityName, Map param){
        handler.globalMap.each{key, values ->
            if(key == entityName){
                values.each(){
                    def fkType = StringUtils.substringBefore(it,"|")
                    def colum = StringUtils.substringAfter(it,"|")
                    def prefix = handler.config.prefix;
                    if(!StringUtils.startsWith(fkType,prefix)){
                        fkType = prefix+fkType
                    }
                    def fkName = StringUtils.uncapitalize(fkType)

                    // get
                    sb.appendLine ""
                    sb.appendLine "\tpublic List<$fkType> get$fkType() {"
                    sb.appendLine "\t\treturn this.$fkName;"
                    sb.appendLine "\t}"

                    // set
                    sb.appendLine ""
                    sb.appendLine "\tpublic void set$fkType(List<$fkType> $fkName) {"
                    sb.appendLine "\t\tthis.$fkName = $fkName;"
                    sb.appendLine "\t}"
                }
            }
        }
    }

}



class RepositoryGenerator extends BaseGenerator implements Generator{

    RepositoryGenerator(GlobalHanlder handler) {
        super(handler)
        this.folder = "repository";
        this.suffix = handler.config.suffix.Repository;
    }

    @Override
    List<Map> generate(DasTable table, File dir) {
        if(!handler.config.repository){
            //handler.log("entity not generate, pass ");
            return null;
        }
        init(table, dir);
        generateImport();
        generateJavaDoc()
        generateBody()
        writeToFile();
        return null
    }

    @Override
    void generateImport() {
        sb.appendLine "package ${basePackage}.repository;";
        sb.appendLine "";
        sb.appendLine "import ${basePackage}.domain.${entityName};";
        sb.appendLine "";
        sb.appendLine "import org.springframework.data.jpa.repository.JpaRepository;";
        sb.appendLine "import org.springframework.data.jpa.repository.JpaSpecificationExecutor;";
        sb.appendLine "";
        sb.appendLine "import org.springframework.stereotype.Repository;";
        sb.appendLine "";
    }

    @Override
    void generateBody() {
        sb.appendLine "@Repository";
        sb.appendLine "public interface ${entityName}Repository extends JpaRepository<${entityName}, Integer>, JpaSpecificationExecutor<${entityName}>{";
        sb.appendLine "";
        sb.appendLine "";
        sb.appendLine "}";
    }

    @Override
    void generateJavaDoc() {
        appendComment()
    }
}


class ServiceGenerator extends BaseGenerator implements Generator{

    ServiceGenerator(GlobalHanlder handler) {
        super(handler)
        this.folder = "service";
        this.suffix = handler.config.suffix.Service
    }


    @Override
    List<Map> generate(DasTable table, File dir) {
        if(!handler.config.service){
            //handler.log("service not generate, pass ");
            return null;
        }
        init(table, dir);
        generateImport();
        generateJavaDoc()
        generateBody()
        writeToFile();
        return null;
    }

    @Override
    void generateImport() {
        sb.appendLine "package ${basePackage}.service;"
        sb.appendLine ""
        if (handler.config.extendBaseService) {
            sb.appendLine "import ${basePackage}.domain.$entityName;"
        }
        sb.appendLine "import org.springframework.data.domain.Page;"
        sb.appendLine ""
    }

    @Override
    void generateBody() {
        sb.appendLine "public interface I${entityName}Service {"
        sb.appendLine ""
        sb.appendLine("\t/**");
        sb.appendLine("\t * create ${uncapEntityName}");
        sb.appendLine("\t * @param ${uncapEntityName}");
        sb.appendLine("\t * @return");
        sb.appendLine("\t */");
        sb.appendLine("\tpublic int create${entityName}(${entityName} ${uncapEntityName});");
        sb.appendLine("\t");
        sb.appendLine("\t/**");
        sb.appendLine("\t * update ${uncapEntityName} ,including logical delete");
        sb.appendLine("\t * @param ${uncapEntityName}");
        sb.appendLine("\t * @param id");
        sb.appendLine("\t * @return");
        sb.appendLine("\t */");
        sb.appendLine("\tpublic int update${entityName}(${entityName} ${uncapEntityName}, Integer id);");
        sb.appendLine("\t");
        sb.appendLine("\t/**");
        sb.appendLine("\t * delete by id (physical delete)");
        sb.appendLine("\t * @param id");
        sb.appendLine("\t * @return");
        sb.appendLine("\t */");
        sb.appendLine("\tpublic int delete${entityName}(Integer id);");
        sb.appendLine("\t");
        sb.appendLine("\t/**");
        sb.appendLine("\t * find by id");
        sb.appendLine("\t * @param id");
        sb.appendLine("\t * @return");
        sb.appendLine("\t */");
        sb.appendLine("\tpublic ${entityName} findById(Integer id) ;");
        sb.appendLine("\t");
        sb.appendLine("\t/**");
        sb.appendLine("\t *  list the  ${uncapEntityName} ,including page ,search , sorts (XX,YY,ZZ)");
        sb.appendLine("\t * @param  ${uncapEntityName}");
        sb.appendLine("\t * @param pageNumber");
        sb.appendLine("\t * @param pageSize");
        sb.appendLine("\t * @param sorts");
        sb.appendLine("\t * @return");
        sb.appendLine("\t */");
        sb.appendLine("\tpublic Page<${entityName}> list(${entityName} ${uncapEntityName}, int pageNumber, int pageSize, String sorts) ;");
        sb.appendLine("\t");

        sb.appendLine "}"
    }

    @Override
    void generateJavaDoc() {
        appendComment()
    }
}



class ServiceImplGenerator extends BaseGenerator implements Generator{

    ServiceImplGenerator(GlobalHanlder handler) {
        super(handler)
        this.folder = "service\\impl";
        this.suffix = handler.config.suffix.ServiceImpl;
    }

    @Override
    List<Map> generate(DasTable table, File dir) {
        if(!handler.config.service){
            //handler.log("service not generate, pass ");
            return null;
        }
        init(table, dir);
        generateImport();
        generateJavaDoc()
        generateBody()
        writeToFile();

        return null;
    }

    @Override
    void generateImport() {
        sb.appendLine "package ${basePackage}.service.impl;"
        sb.appendLine "import java.util.Optional;";
        sb.appendLine "";
        sb.appendLine "import javax.transaction.Transactional;";
        sb.appendLine "";
        sb.appendLine "import org.springframework.beans.factory.annotation.Autowired;";
        sb.appendLine "import org.springframework.data.domain.Pageable;";
        sb.appendLine "import org.springframework.data.domain.Example;";
        sb.appendLine "import org.springframework.data.domain.ExampleMatcher;";
        sb.appendLine "import org.springframework.data.domain.Page;";
        sb.appendLine "";
        sb.appendLine "import org.springframework.stereotype.Service;";
        sb.appendLine "";
        sb.appendLine "import ${basePackage}.domain.${entityName};";
        sb.appendLine "import ${basePackage}.repository.${entityName}Repository;";
        sb.appendLine "import ${basePackage}.constants.Constants;";
        sb.appendLine "import ${basePackage}.service.I${entityName}Service;";
        sb.appendLine "";
        sb.appendLine "\${import}";
        sb.appendLine "import ${basePackage}.utils.PageUtils;";
        sb.appendLine "import ${basePackage}.exception.EpfException;";
        sb.appendLine "import ${basePackage}.constants.Constants;";
    }

    void appendAutowire() {
        fields.forEach {
            if (BaseUtil.isFk(it)) {
                def fkType = BaseUtil.getFkType(it);
                def fkName = BaseUtil.getFkType(it,false,false);

                sb.appendLine ""
                sb.appendLine "\t@Autowired"
                sb.appendLine "\tprivate ${fkType}Repository ${fkName}Repository;"
            }
        }
    }

    void appendQueryFk() {

        sb.appendLine "\t/**";
        sb.appendLine "\t * 处理外键对象";
        sb.appendLine "\t * @param ${uncapEntityName}";
        sb.appendLine "\t */";
        sb.appendLine "\tprivate void queryFk(${entityName} ${uncapEntityName}) {";
        fields.forEach {
            if (BaseUtil.isFk(it)) {
                def fkType = BaseUtil.getFkType(it);
                def fkName = BaseUtil.getFkType(it,false,false);
                def capFkName = BaseUtil.getFkType(it,true,true);
                def uncapFkName = StringUtils.uncapitalize(capFkName)


                importSets.add("import ${basePackage}.domain.${fkType};")
                importSets.add("import ${basePackage}.repository.${fkType}Repository;")

                sb.appendLine "\t    ${fkType} ${fkName}Param = ${uncapEntityName}.get${capFkName}();";
                sb.appendLine "\t    if( ${fkName}Param != null && ${fkName}Param.getId() != null){";
                sb.appendLine "\t        Optional<${fkType}> ${fkName} = ${fkName}Repository.findById(${fkName}Param.getId());";
                sb.appendLine "\t        if(${fkName}.isPresent()){";
                sb.appendLine "\t            ${fkType} ${fkName}Db = ${fkName}.get();";
                sb.appendLine "\t            // 复制参数对象的属性到db对象属性, 用于级联更新";
                sb.appendLine "\t            // NullAwareBeanUtilsBean.mergeNotNullProperties(${fkName}Db, ${fkName}Param);";
                sb.appendLine "\t            ${uncapEntityName}.set${capFkName}(${fkName}Db);";
                sb.appendLine "\t        }else{";
                sb.appendLine "\t            ${uncapEntityName}.set${capFkName}(null);";
                sb.appendLine "\t        }";
                sb.appendLine "\t        //外键id已传入，强依赖的话，这里可以抛异常";
                sb.appendLine "\t        if(${uncapEntityName}.get${capFkName}() == null) {";
                sb.appendLine "\t            Integer ${fkName}Id = ${fkName}Param.getId();";
                def errorMsg = StringEscapeUtils.escapeJava("${uncapEntityName}的依赖${uncapFkName}[id={0}]不存在")
                handler.errorSb.appendLine "${uncapEntityName}.dependency.${uncapFkName} = $errorMsg";

                sb.appendLine "\t            //${uncapEntityName}.dependency.${uncapFkName} = ${uncapEntityName}的依赖${uncapFkName}[id={0}]不存在";
                sb.appendLine "\t            throw new EpfException(\"{${uncapEntityName}.dependency.${uncapFkName}}\",new Object[]{String.valueOf(${fkName}Id)});";
                sb.appendLine "\t        }";
                sb.appendLine "\t    }";

            }
        }

        sb.appendLine "\t";
        sb.appendLine "\t}";
        sb.appendLine "\t";
    }

    @Override
    void generateBody() {
        sb.appendLine "@Service"
        sb.appendLine "public class ${entityName}Service implements I${entityName}Service {"
        sb.appendLine ""
        sb.appendLine "\t@Autowired"
        sb.appendLine "\tprivate ${entityName}Repository ${uncapEntityName}Repository;"

        //注入外键repository
        appendAutowire();


        sb.appendLine "\t"
        sb.appendLine "\t@Override"
        sb.appendLine "\t@Transactional"
        sb.appendLine "\tpublic int create${entityName}(${entityName} ${uncapEntityName}) {"
        // 调用查询外键
        sb.appendLine "\t    queryFk(${uncapEntityName});"
        sb.appendLine "\t    ${uncapEntityName}Repository.save(${uncapEntityName});"
        sb.appendLine "\t    return Constants.RETURN_STATUS_SUCCESS;"
        sb.appendLine "\t}"
        sb.appendLine "\t"
        sb.appendLine "\t@Override"
        sb.appendLine "\t@Transactional"
        sb.appendLine "\tpublic int update${entityName}(${entityName} ${uncapEntityName},Integer id) {"

        // 调用查询外键
        sb.appendLine "\t    queryFk(${uncapEntityName});"

        sb.appendLine "\t    Optional<${entityName}> ${uncapEntityName}Res= ${uncapEntityName}Repository.findById(id);"
        sb.appendLine "\t    if(${uncapEntityName}Res.isPresent()){"
        sb.appendLine "\t            ${uncapEntityName}.setId(id);"
        sb.appendLine "\t            ${uncapEntityName}Repository.save(${uncapEntityName});"
        sb.appendLine "\t            return Constants.RETURN_STATUS_SUCCESS;"
        sb.appendLine "\t    }else{"
        sb.appendLine "\t            return Constants.RETURN_STATUS_FAIL;"
        sb.appendLine "\t    }"
        sb.appendLine "\t"
        sb.appendLine "\t}"
        sb.appendLine "\t"

        //查询外键函数
        appendQueryFk();


        sb.appendLine "\t"
        sb.appendLine "\t@Override"
        sb.appendLine "\t@Transactional"
        sb.appendLine "\tpublic int delete${entityName}(Integer id) {"
        sb.appendLine "\t     Optional<${entityName}> ${uncapEntityName}= ${uncapEntityName}Repository.findById(id);"
        sb.appendLine "\t     if(${uncapEntityName}.isPresent()){"
        sb.appendLine "\t            ${uncapEntityName}Repository.deleteById(id);"
        sb.appendLine "\t            Optional<${entityName}> ${uncapEntityName}Res= ${uncapEntityName}Repository.findById(id);"
        sb.appendLine "\t            if(${uncapEntityName}Res.isPresent()){"
        sb.appendLine "\t                  return Constants.RETURN_STATUS_FAIL;"
        sb.appendLine "\t            }else{"
        sb.appendLine "\t                 return Constants.RETURN_STATUS_SUCCESS;"
        sb.appendLine "\t            }"
        sb.appendLine "\t     }else{"
        sb.appendLine "\t            return Constants.RETURN_STATUS_FAIL;"
        sb.appendLine "\t     }"
        sb.appendLine "\t"
        sb.appendLine "\t}"
        sb.appendLine "\t"
        sb.appendLine "\t@Override"
//        sb.appendLine "\t@Transactional"
        sb.appendLine "\tpublic ${entityName} findById(Integer id) {"
        sb.appendLine "\t       Optional<${entityName} >  ${uncapEntityName} = ${uncapEntityName}Repository.findById(id);"
        sb.appendLine "\t        if(${uncapEntityName}.isPresent()){"
        sb.appendLine "\t            return ${uncapEntityName}.get();"
        sb.appendLine "\t        }else{"
        sb.appendLine "\t            return null;"
        sb.appendLine "\t        }"
        sb.appendLine "\t"
        sb.appendLine "\t       //return ${uncapEntityName}Repository.findOne(id);"
        sb.appendLine "\t}"
        sb.appendLine "\t"
        sb.appendLine "\t@Override"
        sb.appendLine "\tpublic Page<${entityName}> list(${entityName} ${uncapEntityName},int pageNumber,int pageSize,String sorts)  {"
        sb.appendLine "\t"
        sb.appendLine "\t    //add sorts to query"
        sb.appendLine "\t    Page<${entityName}> ${uncapEntityName}Pages =null;"
        sb.appendLine "\t    //Pageable"
        sb.appendLine "\t    Pageable pageable =  PageUtils.pageable(pageNumber,pageSize,sorts);"
        sb.appendLine "\t"
        sb.appendLine "\t    if(${uncapEntityName} == null){"
        sb.appendLine "\t        ${uncapEntityName}Pages = ${uncapEntityName}Repository.findAll(pageable);"
        sb.appendLine "\t    }else{"
        sb.appendLine "\t        //create matcher ,if need ,please modify here"
        sb.appendLine "\t        ExampleMatcher matcher = ExampleMatcher.matchingAll();"
        sb.appendLine "\t        //create instant"
        sb.appendLine "\t        Example<${entityName}> example = Example.of(${uncapEntityName}, matcher);"
        sb.appendLine "\t        ${uncapEntityName}Pages  = ${uncapEntityName}Repository.findAll(example, pageable);"
        sb.appendLine "\t    }"
        sb.appendLine "\t"
        sb.appendLine "\t    return ${uncapEntityName}Pages;"
        sb.appendLine "\t}"
        sb.appendLine "}"

    }

    @Override
    void generateJavaDoc() {
        appendComment();
    }
}



class ServiceTestGenerator extends BaseGenerator implements Generator{

    ServiceTestGenerator(GlobalHanlder handler) {
        super(handler)
        this.folder = "service\\impl";
        this.suffix = handler.config.suffix.ServiceImplTest;

    }

    @Override
    List<Map> generate(DasTable table, File dir) {
        if(!handler.config.serviceTest){
            //handler.log("service Test not generate, pass ");
            return null;
        }
        init(table, dir);
        this.path = StringUtils.replace(path,"main","test")
        generateImport();
        generateJavaDoc()
        generateBody()
        writeToFile();
        return null;
    }

    @Override
    void generateImport() {
        sb.appendLine "package ${basePackage}.service.impl;";
        sb.appendLine "";
        sb.appendLine "import com.dhq.oneoss.cmdb.DomainUtils;";
        sb.appendLine "import ${basePackage}.constants.Constants;";
        sb.appendLine "";
        sb.appendLine "import ${basePackage}.domain.${entityName};";
        sb.appendLine "import com.fasterxml.jackson.databind.ObjectMapper;";
        sb.appendLine "import org.junit.Assert;";
        sb.appendLine "import org.junit.FixMethodOrder;";
        sb.appendLine "import org.junit.Ignore;";
        sb.appendLine "import org.junit.Test;";
        sb.appendLine "import org.junit.runner.RunWith;";
        sb.appendLine "import org.junit.runners.MethodSorters;";
        sb.appendLine "import org.springframework.beans.factory.annotation.Autowired;";
        sb.appendLine "import org.springframework.boot.test.context.SpringBootTest;";
        sb.appendLine "import org.springframework.test.context.junit4.SpringRunner;";
        sb.appendLine "";
        sb.appendLine "\${import}";
        sb.appendLine "";

    }

    @Override
    void generateBody() {
        sb.appendLine "@RunWith(SpringRunner.class)";
        sb.appendLine "@SpringBootTest";
        sb.appendLine "@FixMethodOrder(MethodSorters.DEFAULT)";
        sb.appendLine "public class ${entityName}ServiceTest {";

        sb.appendLine "\t@Autowired";
        sb.appendLine "\tprivate ${entityName}Service ${uncapEntityName}Service;";
        sb.appendLine "\t";
        sb.appendLine "\t";
        sb.appendLine "\tpublic ${entityName} model(){";
        sb.appendLine "\t    return DomainUtils.newRandomInstance(${entityName}.class);";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\tpublic void create${entityName}() {";
        sb.appendLine "\t    ${entityName} model = model();";
        sb.appendLine "\t    Assert.assertEquals(Constants.RETURN_STATUS_SUCCESS, ${uncapEntityName}Service.create${entityName}(model));";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\tpublic void update${entityName}() {";
        sb.appendLine "\t    ${entityName} model = ${uncapEntityName}Service.findById(1);";
        sb.appendLine "\t    model = model == null ? model() : model;";
        sb.appendLine "\t    model.setId(1);";

        setUpdateProperties();

        sb.appendLine "\t    Assert.assertEquals(Constants.RETURN_STATUS_SUCCESS, ${uncapEntityName}Service.update${entityName}(model, model.getId()));";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\tpublic void findById() {";
        sb.appendLine "\t    Assert.assertNotNull(${uncapEntityName}Service.findById(1));";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\t@Ignore";
        sb.appendLine "\tpublic void delete${entityName}() {";
        sb.appendLine "\t    Integer id = 1;";
        sb.appendLine "\t    Assert.assertEquals(Constants.RETURN_STATUS_SUCCESS, ${uncapEntityName}Service.delete${entityName}(id));";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\tpublic void saveManyToOne() {";
        sb.appendLine "\t\ttry{";
        sb.appendLine "\t\t    ${entityName} ${uncapEntityName} = DomainUtils.newRandomInstance(${entityName}.class,true);";
        sb.appendLine "\t\t    ObjectMapper objectMapper = new ObjectMapper();";
        sb.appendLine "\t\t    String json = objectMapper.writeValueAsString(${uncapEntityName});";
        sb.appendLine "\t\t    System.out.println(json);";
        sb.appendLine "\t\t    int res = ${uncapEntityName}Service.create${entityName}(${uncapEntityName});";
        sb.appendLine "\t\t}catch(Exception e){}";
        sb.appendLine "\t}";

        sb.appendLine("}")

    }

    @Override
    void generateJavaDoc() {
        appendComment();
    }
}


class ControllerGenerator extends BaseGenerator implements Generator{

    ControllerGenerator(GlobalHanlder handler) {
        super(handler)
        this.folder = "controller";
        this.suffix = handler.config.suffix.Controller;
    }

    @Override
    List<Map> generate(DasTable table, File dir) {
        if(!handler.config.controller){
            //handler.log("controller not generate, pass ");
            return null;
        }
        init(table, dir);
        generateImport();
        generateJavaDoc()
        generateBody()
        writeToFile();
        return null
    }

    @Override
    void generateImport() {
        sb.appendLine "package ${basePackage}.controller;";
        sb.appendLine ""
        sb.appendLine "import org.springframework.beans.factory.annotation.Autowired;";
        sb.appendLine ""

        sb.appendLine "import org.springframework.web.bind.annotation.PathVariable;";
        sb.appendLine "import org.springframework.web.bind.annotation.RequestBody;";
        sb.appendLine "import org.springframework.web.bind.annotation.RequestMapping;";
        sb.appendLine "import org.springframework.web.bind.annotation.RequestMethod;";
        sb.appendLine "import org.springframework.web.bind.annotation.ResponseBody;";
        sb.appendLine "import org.springframework.web.bind.annotation.RequestParam;";
        sb.appendLine "import org.springframework.web.bind.annotation.RestController;";
        sb.appendLine "import org.springframework.data.domain.Page;";
        sb.appendLine "import javax.validation.Valid;";
        sb.appendLine ""

        sb.appendLine "import com.dhq.oneoss.cmdb.epf.utils.NullAwareBeanUtilsBean;"
        sb.appendLine "import io.swagger.annotations.ApiImplicitParam;";
        sb.appendLine "import io.swagger.annotations.ApiImplicitParams;";
        sb.appendLine "import io.swagger.annotations.ApiOperation;";

        sb.appendLine "import ${basePackage}.constants.Constants;";
        sb.appendLine "import ${basePackage}.domain.${entityName};";
        sb.appendLine "import ${basePackage}.service.I${entityName}Service;";
        sb.appendLine "import ${basePackage}.model.BaseResponse;";
        sb.appendLine "import ${basePackage}.manager.IBaseManager;";
    }

    @Override
    void generateBody() {
        sb.appendLine "@RestController";
        sb.appendLine "@RequestMapping(\"/cmdb/epf/${uncapEntityName}\")";
        sb.appendLine "public class ${entityName}Controller {";

        sb.appendLine "\t@Autowired";
        sb.appendLine "\tprivate IBaseManager baseManager;";
        sb.appendLine "\t@Autowired";
        sb.appendLine "\tprivate I${entityName}Service ${uncapEntityName}Service;";
        sb.appendLine "\t";
        sb.appendLine "\t/**";
        sb.appendLine "\t * Get all list for ${entityName}";
        sb.appendLine "\t * @return";
        sb.appendLine "\t*/";
        sb.appendLine "\t@ApiOperation(value=\"Get list for ${entityName}\", notes=\"Get list for ${entityName}\")";
        sb.appendLine "\t@RequestMapping(value = \"/list\",method = RequestMethod.POST)";
        sb.appendLine "\tpublic BaseResponse<Page<${entityName}>> list(@RequestBody(required = false)  ${entityName} ${uncapEntityName} , @RequestParam(required = false, defaultValue = \"0\") int pageNumber,"
        sb.appendLine "\t                                        @RequestParam(required = false, defaultValue = \"10\") int pageSize,";
        sb.appendLine "\t                                        @RequestParam(required = false) String sorts) {";
        sb.appendLine "\t    Page<${entityName}> ${uncapEntityName}s = ${uncapEntityName}Service.list(${uncapEntityName},pageNumber,pageSize,sorts);";
        sb.appendLine "\t    if(${uncapEntityName}s==null){";
        sb.appendLine "\t            return baseManager.composeDBFailResponse();";
        sb.appendLine "\t    }else{";
        sb.appendLine "\t            return baseManager.composeSuccessBaseResponse(${uncapEntityName}s);";
        sb.appendLine "\t    }";
        sb.appendLine "\t";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t/**";
        sb.appendLine "\t * Add for ${entityName}";
        sb.appendLine "\t * @param ${uncapEntityName}";
        sb.appendLine "\t * @return";
        sb.appendLine "\t */";
        sb.appendLine "\t@ApiOperation(value=\"Create ${entityName}\", notes=\"According to ${entityName} to create\")";
        sb.appendLine "\t@ApiImplicitParam(name = \"${entityName}\", value = \"Detail entity ${entityName}\", required = true, dataType = \"${entityName}\")";
        sb.appendLine "\t@RequestMapping(value = \"/add\",method = RequestMethod.POST)";
        sb.appendLine "\tpublic BaseResponse create(@Valid @RequestBody ${entityName} ${uncapEntityName}) {";
        sb.appendLine "\t    int result = ${uncapEntityName}Service.create${entityName}(${uncapEntityName});";
        sb.appendLine "\t    if(result == Constants.RETURN_STATUS_SUCCESS){";
        sb.appendLine "\t        NullAwareBeanUtilsBean.removeRelations(${uncapEntityName});";
        sb.appendLine "\t        return baseManager.composeSuccessBaseResponse(${uncapEntityName});";
        sb.appendLine "\t    }else{";
        sb.appendLine "\t        return baseManager.composeDBFailResponse();";
        sb.appendLine "\t    }";
        sb.appendLine "\t";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t";
        sb.appendLine "\t/**";
        sb.appendLine "\t * Modify ${entityName} information according to the id";
        sb.appendLine "\t * @param ${uncapEntityName}";
        sb.appendLine "\t * @return";
        sb.appendLine "\t */";
        sb.appendLine "\t@ApiOperation(value=\"update ${entityName} information \", notes=\"According to url id to update ${entityName} information\")";
        sb.appendLine "\t@ApiImplicitParams({";
        sb.appendLine "\t            @ApiImplicitParam(name = \"id\", value = \"${entityName}ID\", required = true, dataType = \"int\",paramType = \"path\"),";
        sb.appendLine "\t            @ApiImplicitParam(name = \"${entityName}\", value = \"entity ${entityName}\", required = true, dataType = \"${entityName}\")";
        sb.appendLine "\t    })";
        sb.appendLine "\t@RequestMapping(value = \"/update/{id}\", method = RequestMethod.PUT)";
        sb.appendLine "\tpublic BaseResponse update(@PathVariable(\"id\")Integer id, @Valid @RequestBody ${entityName} ${uncapEntityName}) {";
        sb.appendLine "\t";
        sb.appendLine "\t    int result = ${uncapEntityName}Service.update${entityName}(${uncapEntityName},id);";
        sb.appendLine "\t    if(result == Constants.RETURN_STATUS_SUCCESS){";
        sb.appendLine "\t";
        sb.appendLine "\t          return baseManager.composeSuccessBaseResponse(${uncapEntityName}Service.findById(id));";
        sb.appendLine "\t     }else{";
        sb.appendLine "\t          return baseManager.composeDBFailResponse();";
        sb.appendLine "\t     }";
        sb.appendLine "\t";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t";
        sb.appendLine "\t/**";
        sb.appendLine "\t * Find ${entityName} according to id";
        sb.appendLine "\t * @param id";
        sb.appendLine "\t * @return";
        sb.appendLine "\t */";
        sb.appendLine "\t@ApiOperation(value=\"Find ${entityName}\", notes=\"According to url id to find ${entityName}\")";
        sb.appendLine "\t@ApiImplicitParam(name = \"id\", value = \"${entityName} ID\", required = true, dataType = \"int\", paramType = \"path\")";
        sb.appendLine "\t@RequestMapping(value = \"/{id}\", method = RequestMethod.GET)";
        sb.appendLine "\tpublic BaseResponse<${entityName}> get(@PathVariable(\"id\")Integer id) {";
        sb.appendLine "\t    ${entityName} ${uncapEntityName} = ${uncapEntityName}Service.findById(id);";
        sb.appendLine "\t    if(${uncapEntityName}==null){";
        sb.appendLine "\t          return baseManager.composeDBFailResponse();";
        sb.appendLine "\t    }else{";
        sb.appendLine "\t          return baseManager.composeSuccessBaseResponse(${uncapEntityName});";
        sb.appendLine "\t    }";
        sb.appendLine "\t";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t";
        sb.appendLine "\t/**";
        sb.appendLine "\t * Delete ${entityName} according to id";
        sb.appendLine "\t * @param id";
        sb.appendLine "\t * @return";
        sb.appendLine "\t */";
        sb.appendLine "\t@ApiOperation(value=\"Delete ${entityName}\", notes=\"According to id to delete ${entityName}\")";
        sb.appendLine "\t@ApiImplicitParam(name = \"id\", value = \"${entityName} ID\", required = true, dataType = \"int\", paramType = \"path\")";
        sb.appendLine "\t@RequestMapping(value = \"/delete/{id}\", method = RequestMethod.DELETE)";
        sb.appendLine "\tpublic BaseResponse delete(@PathVariable(\"id\")Integer id) {";
        sb.appendLine "\t";
        sb.appendLine "\t    int result = ${uncapEntityName}Service.delete${entityName}(id);";
        sb.appendLine "\t    if(result == Constants.RETURN_STATUS_SUCCESS){";
        sb.appendLine "\t          return baseManager.composeCommonSuccessResponse();";
        sb.appendLine "\t    }else{";
        sb.appendLine "\t          return baseManager.composeDBFailResponse();";
        sb.appendLine "\t    }";
        sb.appendLine "\t}";

        sb.appendLine("}")
    }

    @Override
    void generateJavaDoc() {
        appendComment();
    }
}

class ControllerTestGenerator extends BaseGenerator implements Generator{

    ControllerTestGenerator(GlobalHanlder handler) {
        super(handler)
        this.folder = "controller";
        this.suffix = handler.config.suffix.ControllerTest;
    }

    @Override
    List<Map> generate(DasTable table, File dir) {
        if(!handler.config.controllerTest){
            //handler.log("controller Test not generate, pass ");
            return null;
        }
        init(table, dir);
        this.path = StringUtils.replace(path,"main","test")
        generateImport();
        generateJavaDoc()
        generateBody()
        writeToFile();
        return null;
    }

    @Override
    void generateImport() {
        sb.appendLine "package ${basePackage}.controller;";
        sb.appendLine "";
        sb.appendLine "import com.dhq.oneoss.cmdb.DomainUtils;";
        sb.appendLine "import ${basePackage}.domain.${entityName};";
        sb.appendLine "import com.fasterxml.jackson.databind.ObjectMapper;";
        sb.appendLine "import org.hamcrest.core.StringContains;";
        sb.appendLine "import org.junit.FixMethodOrder;";
        sb.appendLine "import org.junit.Ignore;";
        sb.appendLine "import org.junit.Test;";
        sb.appendLine "import org.junit.runner.RunWith;";
        sb.appendLine "import org.junit.runners.MethodSorters;";
        sb.appendLine "import org.springframework.beans.factory.annotation.Autowired;";
        sb.appendLine "import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;";
        sb.appendLine "import org.springframework.boot.test.context.SpringBootTest;";
        sb.appendLine "import org.springframework.http.MediaType;";
        sb.appendLine "import org.springframework.test.context.junit4.SpringRunner;";
        sb.appendLine "import org.springframework.test.web.servlet.MockMvc;";
        sb.appendLine "import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;";
        sb.appendLine "import org.springframework.test.web.servlet.result.MockMvcResultHandlers;";
        sb.appendLine "import org.springframework.test.web.servlet.result.MockMvcResultMatchers;";
        sb.appendLine "";
        sb.appendLine "\${import}";
        sb.appendLine "";
    }

    @Override
    void generateBody() {
        sb.appendLine "@RunWith(SpringRunner.class)";
        sb.appendLine "@SpringBootTest";
        sb.appendLine "@AutoConfigureMockMvc";
        sb.appendLine "@FixMethodOrder(MethodSorters.DEFAULT)";
        sb.appendLine "public class ${entityName}ControllerTest {";

        sb.appendLine "\t@Autowired";
        sb.appendLine "\tprivate MockMvc mockMvc;";
        sb.appendLine "\t";
        sb.appendLine "\tprivate ${entityName} model(){";
        sb.appendLine "\t    ${entityName} model = DomainUtils.newRandomInstance(${entityName}.class);";
        sb.appendLine "\t    return model;";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\tpublic void create() throws Exception{";
        sb.appendLine "\t    String json = new ObjectMapper().writeValueAsString(model());";
        sb.appendLine "\t    mockMvc.perform(MockMvcRequestBuilders.post(\"/cmdb/epf/${uncapEntityName}/add\")";
        sb.appendLine "\t            .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(MockMvcResultMatchers.status().isOk())";
        sb.appendLine "\t            .andDo(MockMvcResultHandlers.print());";
        sb.appendLine "\t";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\tpublic void saveManyToOne() throws Exception{";
        sb.appendLine "\t    ${entityName} ${uncapEntityName} = DomainUtils.newRandomInstance(${entityName}.class,true);";
        sb.appendLine "\t    ObjectMapper objectMapper = new ObjectMapper();";
        sb.appendLine "\t    String json = objectMapper.writeValueAsString(${uncapEntityName});";
        sb.appendLine "\t    System.out.println(json);";
        sb.appendLine "\t    mockMvc.perform(MockMvcRequestBuilders.post(\"/cmdb/epf/${uncapEntityName}/add\")";
        sb.appendLine "\t            .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(MockMvcResultMatchers.status().isOk())";
        sb.appendLine "\t            .andDo(MockMvcResultHandlers.print());";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\tpublic void list() throws Exception{";
        sb.appendLine "\t    mockMvc.perform(MockMvcRequestBuilders.post(\"/cmdb/epf/${uncapEntityName}/list\")).andExpect(MockMvcResultMatchers.status().isOk());";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\tpublic void get() throws Exception{";
        sb.appendLine "\t    mockMvc.perform(MockMvcRequestBuilders.get(\"/cmdb/epf/${uncapEntityName}/1\")).andExpect(MockMvcResultMatchers.status().isOk());";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\tpublic void update() throws Exception{";
        sb.appendLine "\t    ${entityName} model = model();";
        sb.appendLine "\t    model.setId(1);";

        setUpdateProperties();

        sb.appendLine "\t    String json = new ObjectMapper().writeValueAsString(model);";
        sb.appendLine "\t    mockMvc.perform(MockMvcRequestBuilders.put(\"/cmdb/epf/${uncapEntityName}/update/1\").contentType(MediaType.APPLICATION_JSON)";
        sb.appendLine "\t            .content(json)).andExpect(MockMvcResultMatchers.status().isOk()).andDo(MockMvcResultHandlers.print());";
        sb.appendLine "\t}";
        sb.appendLine "\t";
        sb.appendLine "\t@Test";
        sb.appendLine "\t@Ignore";
        sb.appendLine "\tpublic void delete() throws Exception{";
        sb.appendLine "\t    mockMvc.perform(MockMvcRequestBuilders.delete(\"/cmdb/epf/${uncapEntityName}/delete/1\")).andExpect(MockMvcResultMatchers.status().isOk())";
        sb.appendLine "\t            .andExpect(MockMvcResultMatchers.content().string(StringContains.containsString(\"Request Successful\")))";
        sb.appendLine "\t            .andDo(MockMvcResultHandlers.print());";
        sb.appendLine "\t";
        sb.appendLine "\t}";

        sb.appendLine "}"
    }

    @Override
    void generateJavaDoc() {
        appendComment();
    }
}

class VueApiGenerator extends BaseGenerator implements Generator {

    VueApiGenerator(GlobalHanlder handler) {
        super(handler)
        this.folder = "vue";
        this.suffix = handler.config.suffix.VueApi;
    }

    @Override
    List<Map> generate(DasTable table, File dir) {
        if (!handler.config.VueApi) {
            return null;
        }
        init(table, dir);
        generateImport();
        generateJavaDoc()
        generateBody()
        writeToFile();
        return null;
    }

    @Override
    void generateImport() {

    }

    @Override
    void generateBody() {
        sb.append "import request from '@/utils/request'"
        sb.appendLine ""
        sb.appendLine "export function fetchList(query) {"
        sb.appendLine "  return request({"
        sb.appendLine "    url: '/cmdb/epf/${uncapEntityName}/list',"
        sb.appendLine "    method: 'post',"
        sb.appendLine "    params: query"
        sb.appendLine "  })"
        sb.appendLine "}"
        sb.appendLine ""
        sb.appendLine "export function fetch${entityName}(id) {"
        sb.appendLine "  return request({"
        sb.appendLine "    url: '/cmdb/epf/${uncapEntityName}/:id',"
        sb.appendLine "    method: 'get',"
        sb.appendLine "    params: { id }"
        sb.appendLine "  })"
        sb.appendLine "}"
        sb.appendLine ""
        sb.appendLine "export function create${entityName}(data) {"
        sb.appendLine ""
        sb.appendLine "  return request({"
        sb.appendLine "    url: '/cmdb/epf/${uncapEntityName}/add',"
        sb.appendLine "    method: 'post',"
        sb.appendLine "    data"
        sb.appendLine "  })"
        sb.appendLine "}"
        sb.appendLine ""
        sb.appendLine "export function update${entityName}(data) {"
        sb.appendLine "  return request({"
        sb.appendLine "    url: '/cmdb/epf/${uncapEntityName}/update/:data.id',"
        sb.appendLine "    method: 'put',"
        sb.appendLine "    data"
        sb.appendLine "  })"
        sb.appendLine "}"
        sb.appendLine ""
        sb.appendLine "export function delete${entityName}(data) {"
        sb.appendLine "  return request({"
        sb.appendLine "    url: '/cmdb/epf/${uncapEntityName}/delete/:data.id',"
        sb.appendLine "    method: 'delete',"
        sb.appendLine "    data"
        sb.appendLine "  })"
        sb.appendLine "}"
    }

    @Override
    void generateJavaDoc() {

    }
}



class VueViewGenerator extends BaseGenerator implements Generator {

    VueViewGenerator(GlobalHanlder handler) {
        super(handler)
        this.folder = "view";
        this.suffix = handler.config.suffix.VueView;
    }

    @Override
    List<Map> generate(DasTable table, File dir) {
        if (!handler.config.VueView) {
             null;
        }
        init(table, dir);
        generateImport();
        generateJavaDoc()
        generateBody()
        writeToFile();
        return null;
    }

    @Override
    void generateImport() {
        sb.append "<template>"
        sb.appendLine "  <div class=\"app-container\">"
        appendSearchBar()
        appendColsTmpl()
        appendPager()
        appendDialog()
        sb.appendLine "  </div>"
        sb.appendLine "</template>"
        sb.appendLine ""


    }

    void appendDialog(){
        if(hasOne){
            sb.appendLine "    <!-- //parent dialog -->"
            sb.appendLine "    <el-dialog :title=\"prefix?\$t('route.'+prefix):''\" :visible.sync=\"dialogParentVisible\">"
            sb.appendLine "      <parent  :obj=\"parent\" :prefix='prefix'>"
            sb.appendLine "      </parent>"
            sb.appendLine "    </el-dialog>"
            sb.appendLine ""
        }
        if(hasMany) {
            sb.appendLine "    <!-- //children dialog -->"
            sb.appendLine "    <el-dialog title=\"\" class= \"theme1\" width=\"60%\" :visible.sync=\"dialogChildrenVisible\">"
            sb.appendLine "      <children  :obj=\"children\">"
            sb.appendLine "      </children>"
            sb.appendLine "    </el-dialog>"
            sb.appendLine ""
        }

        sb.appendLine "    <el-dialog :title=\"i18nTitle(dialogStatus)\" :visible.sync=\"dialogFormVisible\">"
        sb.appendLine "      <el-form"
        sb.appendLine "        ref=\"dataForm\""
        sb.appendLine "        :rules=\"rules\""
        sb.appendLine "        :model=\"temp\""
        sb.appendLine "        label-position=\"left\""
        sb.appendLine "        label-width=\"120px\""
        sb.appendLine "        style=\"width: 400px; margin: 0 auto;\""
        sb.appendLine "      >"

        appendFormItems()

        sb.appendLine "      </el-form>"
        sb.appendLine ""

        sb.appendLine "      <div slot=\"footer\" class=\"dialog-footer\">"
        sb.appendLine "        <el-button @click=\"dialogFormVisible = false\">{{\$t('table.cancel')}}</el-button>"
        sb.appendLine "        <el-button type=\"primary\" @click=\"dialogStatus==='create'?createData():updateData()\">{{\$t('table.confirm')}}</el-button>"
        sb.appendLine "      </div>"
        sb.appendLine "    </el-dialog>"
        sb.appendLine ""

    }

    void appendSearchBar(){
        def firstCol = fields[1].name
        def comment = fields[1].comment
        sb.appendLine "    <div class=\"filter-container\">"
        sb.appendLine "      <el-input"
        sb.appendLine "        v-model=\"listQuery.id\""
        sb.appendLine "        placeholder=\"Id\""
        sb.appendLine "        style=\"width: 200px;\""
        sb.appendLine "        class=\"filter-item\""
        sb.appendLine "        @keyup.enter.native=\"handleFilter\""
        sb.appendLine "      />"
        sb.appendLine ""
        sb.appendLine "      <el-input"
        sb.appendLine "        v-model=\"listQuery.${firstCol}\""
        sb.appendLine "        placeholder=\"search ${comment}\""
        sb.appendLine "        style=\"width: 200px;\""
        sb.appendLine "        class=\"filter-item\""
        sb.appendLine "        @keyup.enter.native=\"handleFilter\""
        sb.appendLine "      />"
        sb.appendLine ""
        sb.appendLine "      <el-button"
        sb.appendLine "        v-waves"
        sb.appendLine "        class=\"filter-item\""
        sb.appendLine "        type=\"primary\""
        sb.appendLine "        icon=\"el-icon-search\""
        sb.appendLine "        @click=\"handleFilter\""
        sb.appendLine "      >{{ \$t('table.search') }}</el-button>"
        sb.appendLine "      <el-button"
        sb.appendLine "        class=\"filter-item\""
        sb.appendLine "        style=\"margin-left: 10px;\""
        sb.appendLine "        type=\"primary\""
        sb.appendLine "        icon=\"el-icon-edit\""
        sb.appendLine "        @click=\"handleCreate\""
        sb.appendLine "      >{{ \$t('table.add') }}</el-button>"
        sb.appendLine "      <el-button"
        sb.appendLine "        v-waves"
        sb.appendLine "        :loading=\"downloadLoading\""
        sb.appendLine "        class=\"filter-item\""
        sb.appendLine "        type=\"primary\""
        sb.appendLine "        icon=\"el-icon-download\""
        sb.appendLine "        @click=\"handleDownload\""
        sb.appendLine "      >{{ \$t('table.export') }}</el-button>"
        sb.appendLine ""
        sb.appendLine "      <el-checkbox"
        sb.appendLine "        v-model=\"showMore\""
        sb.appendLine "        class=\"filter-item\""
        sb.appendLine "        style=\"margin-left:15px;\""
        sb.appendLine "        @change=\"tableKey=tableKey+1\""
        sb.appendLine "      >{{ \$t('common.more') }}</el-checkbox>"
        sb.appendLine "    </div>"
        sb.appendLine ""
    }

    void appendPager(){
        sb.appendLine "	    <pagination"
        sb.appendLine "	      v-show=\"total>0\""
        sb.appendLine "	      :total=\"total\""
        sb.appendLine "	      :page.sync=\"listQuery.page\""
        sb.appendLine "	      :limit.sync=\"listQuery.limit\""
        sb.appendLine "	      @pagination=\"getList\""
        sb.appendLine "	    />"
        sb.appendLine ""
    }

    void appendColsTmpl(){
        sb.appendLine "    <el-table"
        sb.appendLine "          :key=\"tableKey\""
        sb.appendLine "          v-loading=\"listLoading\""
        sb.appendLine "          :data=\"list\""
        sb.appendLine "          border"
        sb.appendLine "          fit"
        sb.appendLine "          highlight-current-row"
        sb.appendLine "          style=\"width: 100%;\""
        sb.appendLine "          @sort-change=\"sortChange\""
        sb.appendLine "        >"
        fields.each {
            def name = it.name;
            if(BaseUtil.isFk(it)) {
                def fkType = BaseUtil.getFkType(it)
                fkType = BaseUtil.specailHandle(fkType)
                def prop = StringUtils.uncapitalize(fkType)

                sb.appendLine "      <el-table-column :label=\"\$t('route.$prop')+'ID'\" width=\"100px\" align=\"center\">"
                sb.appendLine "        <template slot-scope=\"scope\">"
                sb.appendLine "          <el-button v-if=\"scope.row.$prop\" type=\"primary\" size=\"mini\" @click=\"showParent(scope.row.$prop,'$prop')\">{{ scope.row.$prop?\$t('route.$prop')+scope.row.${prop}.id:\"\" }}</el-button>"
                sb.appendLine "        </template>"
                sb.appendLine "      </el-table-column>"
            }else{
                if(name == 'id'){
                    sb.appendLine "      <el-table-column"
                    sb.appendLine "        :label=\"\$t('${uncapEntityName}.id')\""
                    sb.appendLine "        prop=\"id\""
                    sb.appendLine "        sortable=\"custom\""
                    sb.appendLine "        align=\"center\""
                    sb.appendLine "        width=\"80\""
                    sb.appendLine "      >"
                }else if(handler.config.commonProp.contains(name)) {
                    sb.appendLine "      <el-table-column"
                    sb.appendLine "        v-if=\"showMore\""
                    sb.appendLine "        :label=\"\$t('common.$name')\""
                    sb.appendLine "        width=\"150px\""
                    sb.appendLine "        align=\"center\""
                    sb.appendLine "      >"
                }else {
                    def labelPrefix = uncapEntityName
                    if(handler.config.showProp.contains(name)){
                        labelPrefix = 'common'
                    }
                    sb.appendLine "      <el-table-column :label=\"\$t('${labelPrefix}.$name')\" width=\"150px\" align=\"center\">"
                }
                sb.appendLine "        <template slot-scope=\"scope\">"
                if(it.type == 'Date'){
                    sb.appendLine "          <span>{{ scope.row.$name | parseTime('{y}-{m}-{d} {h}:{i}') }}</span>"
                }else{
                    sb.appendLine "          <span class=\"link-type\" @click=\"handleUpdate(scope.row)\">{{ scope.row.$name }}</span>"
                }
                sb.appendLine "        </template>"
                sb.appendLine "      </el-table-column>"
            }
            sb.appendLine ""
        }
        if(handler.globalMap.containsKey(entityName)) {
            // 一对多列表
            sb.appendLine "      <el-table-column label=\"关联\" width=\"115px\" align=\"center\">"
            sb.appendLine "        <template slot-scope=\"scope\">"
            sb.appendLine "          <el-button type=\"primary\" size=\"mini\" @click=\"showChildren(scope.row)\">查看关联</el-button>"
            sb.appendLine "        </template>"
            sb.appendLine "      </el-table-column>"
            sb.appendLine ""
        }

        sb.appendLine "      <el-table-column"
        sb.appendLine "        :label=\"\$t('table.actions')\""
        sb.appendLine "        align=\"center\""
        sb.appendLine "        width=\"230\""
        sb.appendLine "        class-name=\"small-padding fixed-width\""
        sb.appendLine "      >"
        sb.appendLine "        <template slot-scope=\"{row}\">"
        sb.appendLine "          <el-button type=\"primary\" size=\"mini\" @click=\"handleUpdate(row)\">{{\$t('table.edit')}}</el-button>"
        sb.appendLine "          <el-button type=\"danger\" size=\"mini\" @click=\"handleDelete(row)\">{{\$t('table.delete')}}</el-button>"
        sb.appendLine "        </template>"
        sb.appendLine "      </el-table-column>"
        sb.appendLine "    </el-table>"
        sb.appendLine ""



    }

    void appendFormItems(){
        fields.each{
            def name = it.name;
            if(handler.config.commonProp.contains(name) || name == 'id'){
                return
            }

            if(BaseUtil.isFk(it)){
                def fkType = BaseUtil.getFkType(it)
                fkType = BaseUtil.specailHandle(fkType)
                def prop = StringUtils.uncapitalize(fkType)
                sb.appendLine "        <el-form-item :label=\"\$t('route.${prop}')+'ID'\" prop=\"${prop}Id\">"
                sb.appendLine "          <el-input v-model=\"temp.${prop}.id\" />"
                sb.appendLine "        </el-form-item>"
            }else {
                def labelPrefix = uncapEntityName
                if(handler.config.showProp.contains(name)){
                    labelPrefix = 'common'
                }
                sb.appendLine "        <el-form-item :label=\"\$t('${labelPrefix}.$name')\" prop=\"$name\">"
                if(it.type == 'Date'){
                    sb.appendLine "          <el-date-picker v-model=\"temp.$name\" type=\"datetime\" :placeholder=\"\$t('date.placeholder')\"/>"
                }else{
                    sb.appendLine "          <el-input v-model=\"temp.$name\"/>"

                }
                sb.appendLine "        </el-form-item>"
            }
            sb.appendLine ""
        }

    }

    @Override
    void generateBody() {

        sb.appendLine "<script>"
        sb.appendLine "import {"
        sb.appendLine "  fetchList,"
        sb.appendLine "  create${entityName},"
        sb.appendLine "  update${entityName},"
        sb.appendLine "  delete${entityName}"
        sb.appendLine "} from \"@/api/epf/${uncapEntityName}\";"
        sb.appendLine "import waves from \"@/directive/waves\"; // waves directive"
        sb.appendLine "import { parseTime, lowercaseFirst } from \"@/utils\";"
        sb.appendLine "import Pagination from \"@/components/Pagination\"; // secondary package based on el-pagination"
        if(hasOne){
            sb.appendLine "import Parent from \"@/components/Parent\"; // 父对象标签"
        }
        if(hasMany){
            sb.appendLine "import Children from \"@/components/Children\"; // 子列表标签"
        }
        sb.appendLine ""
        sb.appendLine "const calendarTypeOptions = ["
        sb.appendLine "  { key: \"CN\", display_name: \"China\" },"
        sb.appendLine "  { key: \"US\", display_name: \"USA\" },"
        sb.appendLine "  { key: \"JP\", display_name: \"Japan\" },"
        sb.appendLine "  { key: \"EU\", display_name: \"Eurozone\" }"
        sb.appendLine "];"
        sb.appendLine ""
        sb.appendLine "// arr to obj, such as { CN : \"China\", US : \"USA\" }"
        sb.appendLine "const calendarTypeKeyValue = calendarTypeOptions.reduce((acc, cur) => {"
        sb.appendLine "  acc[cur.key] = cur.display_name;"
        sb.appendLine "  return acc;"
        sb.appendLine "}, {});"
        sb.appendLine ""
        sb.appendLine "export default {"
        sb.appendLine "  name: \"${entityName}\","
        sb.appendLine "  components: { Pagination${hasOne?', Parent':''}${hasMany?', Children':''}},"
        sb.appendLine "  directives: { waves },"
        sb.appendLine "  filters: {"
        sb.appendLine "    statusFilter(status) {"
        sb.appendLine "      const statusMap = {"
        sb.appendLine "        published: \"success\","
        sb.appendLine "        draft: \"info\","
        sb.appendLine "        deleted: \"danger\""
        sb.appendLine "      };"
        sb.appendLine "      return statusMap[status];"
        sb.appendLine "    },"
        sb.appendLine "    typeFilter(type) {"
        sb.appendLine "      return calendarTypeKeyValue[type];"
        sb.appendLine "    }"
        sb.appendLine "  },"
        sb.appendLine "  computed:{"
        sb.appendLine "      i18nTitle(){ "
        sb.appendLine "        return  function(type){"
        sb.appendLine "          return type ? this.\$i18n.t('table.'+ this.textMap[type]) : \"\";"
        sb.appendLine "        } "
        sb.appendLine "      }"
        sb.appendLine "  },"
        sb.appendLine "  data() {"
        sb.appendLine "    return {"
        sb.appendLine "      tableKey: 0,"
        sb.appendLine "      list: null,"
        sb.appendLine "      total: 0,"
        if(hasOne){
            sb.appendLine "      // 用于展示多对一的父端"
            sb.appendLine "      parent: {},"
            sb.appendLine "      prefix: '',"
        }
        if(hasMany){
            sb.appendLine "      children: {},"
        }
        sb.appendLine "      listLoading: true,"
        sb.appendLine "      listQuery: {"
        sb.appendLine "        page: 1,"
        sb.appendLine "        limit: 10,"
        sb.appendLine "        title: undefined,"
        sb.appendLine "        type: undefined,"
        sb.appendLine "        sort: \'id|desc\'"
        sb.appendLine "      },"
        sb.appendLine "      calendarTypeOptions,"
        sb.appendLine "      sortOptions: ["
        sb.appendLine "        { label: \'ID Ascending\', key: \'id|asc\' },"
        sb.appendLine "        { label: \'ID Descending\', key: \'id|desc\' }"
        sb.appendLine "      ],"
        sb.appendLine "      showMore: false,"
        sb.appendLine "      temp: {"
        sb.appendLine "        id: undefined,"
        sb.appendLine "        type: undefined,"

        appendTempObjFk()

        sb.appendLine "      },"
        sb.appendLine "      dialogFormVisible: false,"
        if(hasOne){
            sb.appendLine "      dialogParentVisible: false,"
        }
        if(hasMany){
            sb.appendLine "      dialogChildrenVisible: false,"
        }
        sb.appendLine "      dialogStatus: \'\',"
        sb.appendLine "      textMap: {"
        sb.appendLine "        update: \'edit\',"
        sb.appendLine "        create: \'add\'"
        sb.appendLine "      },"
        sb.appendLine "      rules: {"
        sb.appendLine "        type: ["
        sb.appendLine "          { required: true, message: \"type is required\", trigger: \"change\" }"
        sb.appendLine "        ],"
        sb.appendLine "        timestamp: ["
        sb.appendLine "          {"
        sb.appendLine "            type: \"date\","
        sb.appendLine "            required: true,"
        sb.appendLine "            message: \"timestamp is required\","
        sb.appendLine "            trigger: \"change\""
        sb.appendLine "          }"
        sb.appendLine "        ],"
        sb.appendLine "        title: ["
        sb.appendLine "          { required: true, message: \"title is required\", trigger: \"blur\" }"
        sb.appendLine "        ]"
        sb.appendLine "      },"
        sb.appendLine "      downloadLoading: false"
        sb.appendLine "    };"
        sb.appendLine "  },"
        sb.appendLine "  created() {"
        sb.appendLine "    this.getList();"
        sb.appendLine "  },"
        sb.appendLine "  methods: {"
        sb.appendLine "    getList() {"
        sb.appendLine "      this.listLoading = true;"
        sb.appendLine "      fetchList(this.listQuery).then(response => {"
        sb.appendLine "        this.list = response.data.items;"
        sb.appendLine "        this.total = response.data.total;"
        sb.appendLine "        this.listLoading = false;"
        sb.appendLine "      });"
        sb.appendLine "    },"
        sb.appendLine "    handleFilter() {"
        sb.appendLine "      this.listQuery.page = 1;"
        sb.appendLine "      this.getList();"
        sb.appendLine "    },"
        sb.appendLine "    sortChange(data) {"
        sb.appendLine "      const { prop, order } = data;"
        sb.appendLine "      if (prop === \"id\") {"
        sb.appendLine "        this.sortByID(order);"
        sb.appendLine "      }"
        sb.appendLine "    },"
        sb.appendLine "    sortByID(order) {"
        sb.appendLine "      if (order === \"ascending\") {"
        sb.appendLine "        this.listQuery.sort = \'id|asc\';"
        sb.appendLine "      } else {"
        sb.appendLine "        this.listQuery.sort = \'id|desc\';"
        sb.appendLine "      }"
        sb.appendLine "      this.handleFilter();"
        sb.appendLine "    },"
        sb.appendLine "    resetTemp() {"
        sb.appendLine "      this.temp = {"
        sb.appendLine "        id: undefined,"
        appendTempObjFk()
        sb.appendLine "      };"
        sb.appendLine "    },"
        sb.appendLine "    handleCreate() {"
        sb.appendLine "      this.resetTemp();"
        sb.appendLine "      this.dialogStatus = \"create\";"
        sb.appendLine "      this.dialogFormVisible = true;"
        sb.appendLine "      this.\$nextTick(() => {"
        sb.appendLine "        this.\$refs[\"dataForm\"].clearValidate();"
        sb.appendLine "      });"
        sb.appendLine "    },"
        sb.appendLine "    createData() {"
        sb.appendLine "      this.\$refs[\"dataForm\"].validate(valid => {"
        sb.appendLine "        if (valid) {"

        appendFkIdNotNull()

        sb.appendLine "          create${entityName}(params).then((res) => {"
        sb.appendLine "            //this.list.unshift(res.data);"
        sb.appendLine "            this.getList();"
        sb.appendLine "            this.dialogFormVisible = false;"
        sb.appendLine "            this.\$notify({"
        sb.appendLine "              title: this.\$i18n.t('table.tip.success'),"
        sb.appendLine "              message: this.\$i18n.t('table.tip.createSuccess'),"
        sb.appendLine "              type: \"success\","
        sb.appendLine "              duration: 2000"
        sb.appendLine "            });"
        sb.appendLine "          });"
        sb.appendLine "        }"
        sb.appendLine "      });"
        sb.appendLine "    },"
        sb.appendLine "    handleUpdate(row) {"
        sb.appendLine "      this.resetTemp();"
        sb.appendLine "      this.temp = Object.assign({},this.temp, row); // copy obj"
        sb.appendLine "      // this.temp.timestamp = new Date(this.temp.timestamp);"
        sb.appendLine "      this.dialogStatus = \"update\";"
        sb.appendLine "      this.dialogFormVisible = true;"
        sb.appendLine "      this.\$nextTick(() => {"
        sb.appendLine "        this.\$refs[\"dataForm\"].clearValidate();"
        sb.appendLine "      });"
        sb.appendLine "    },"
        sb.appendLine "    updateData() {"
        sb.appendLine "      this.\$refs[\"dataForm\"].validate(valid => {"
        sb.appendLine "        if (valid) {"
        sb.appendLine "          const tempData = Object.assign({}, this.temp);"

        appendFkIdNotNull('tempData')
        sb.appendLine "          //console.log(tempData)"
        sb.appendLine "          update${entityName}(params).then((res) => {"
        sb.appendLine "            for (const v of this.list) {"
        sb.appendLine "              if (v.id === this.temp.id) {"
        sb.appendLine "                const index = this.list.indexOf(v);"
        sb.appendLine "                this.list.splice(index, 1, res.data);"
        sb.appendLine "                break;"
        sb.appendLine "              }"
        sb.appendLine "            }"
//        sb.appendLine "            this.getList();"
        sb.appendLine "            this.dialogFormVisible = false;"
        sb.appendLine "            this.\$notify({"
        sb.appendLine "              title: this.\$i18n.t('table.tip.success'),"
        sb.appendLine "              message: this.\$i18n.t('table.tip.updateSuccess'),"
        sb.appendLine "              type: \"success\","
        sb.appendLine "              duration: 2000"
        sb.appendLine "            });"
        sb.appendLine "          });"
        sb.appendLine "        }"
        sb.appendLine "      });"
        sb.appendLine "    },"
        sb.appendLine "    handleDelete(row) {"
        sb.appendLine "      let self = this;"
        sb.appendLine "      let msg = self.\$i18n.t('table.tip.deleteMsg'); //确定删除？"
        sb.appendLine "      let tip = self.\$i18n.t('table.tip.tip'); //提示"
        sb.appendLine "      this.\$confirm(msg, tip, {"
        sb.appendLine "          confirmButtonText: self.\$i18n.t('table.confirm'),"
        sb.appendLine "          cancelButtonText: self.\$i18n.t('table.cancel'),"
        sb.appendLine "          type: 'warning'"
        sb.appendLine "        }).then(async () => { // 这里加个 async，可以查下相关文档 async...await"
        sb.appendLine "            let res = await self.confirmDelete(row);"
        sb.appendLine "            if(res.resultCode===\"0\"){"
        sb.appendLine "                self.\$notify({"
        sb.appendLine "                  title: self.\$i18n.t('table.tip.success'),"
        sb.appendLine "                  message: self.\$i18n.t('table.tip.deleteSuccess'),"
        sb.appendLine "                  type: \"success\","
        sb.appendLine "                  duration: 2000"
        sb.appendLine "                });"
        sb.appendLine "                self.getList()"
        sb.appendLine "            }"
        sb.appendLine "            else{"
        sb.appendLine "                self.\$notify({"
        sb.appendLine "                  title: self.\$i18n.t('table.tip.fail'),"
        sb.appendLine "                  message: self.\$i18n.t('table.tip.deleteFail'),"
        sb.appendLine "                  type: \"error\","
        sb.appendLine "                  duration: 2000"
        sb.appendLine "                });"
        sb.appendLine "            }"
        sb.appendLine "        }).catch((e) => {"
        sb.appendLine "            console.log(e);"
        sb.appendLine "        });"
        sb.appendLine "    },"
        sb.appendLine "    async confirmDelete(row) {"
        sb.appendLine "      row.enabled = 0;"
        sb.appendLine "      return update${entityName}(row).then((data)=>{"
        sb.appendLine "        return data;"
        sb.appendLine "      })"
        sb.appendLine "    },"
        if (hasOne){
            sb.appendLine "    showParent(obj,prefix){"
            sb.appendLine "      this.dialogParentVisible = true;"
            sb.appendLine "      this.parent= obj;"
            sb.appendLine "      this.prefix = prefix;"
            sb.appendLine "    },"
        }
        if(hasMany){
            sb.appendLine "    showChildren(obj){"
            sb.appendLine "      this.dialogChildrenVisible = true;"
            sb.appendLine "      this.children= obj;"
            sb.appendLine "    },"
        }
        sb.appendLine "    handleDownload() {"
        sb.appendLine "      this.downloadLoading = true;"
        sb.appendLine "      import(\"@/vendor/Export2Excel\").then(excel => {"
        sb.appendLine "        const tHeader = ["


        appendDownloadProps()

        sb.appendLine "        ];"
        sb.appendLine ""
        sb.appendLine "        const filterVal = ["

        appendDownloadProps()

        sb.appendLine "        ];"
        sb.appendLine "        let self = this;"
        sb.appendLine "        let prefix = lowercaseFirst(this.\$options.name)+'.';"
        sb.appendLine "        const tHeaderI18n = tHeader.map(function(val){ return self.\$i18n.t(prefix+val)})"
        sb.appendLine ""
        sb.appendLine "        const data = this.formatJson(filterVal, this.list);"
        sb.appendLine "        excel.export_json_to_excel({"
        sb.appendLine "          header: tHeaderI18n,"
        sb.appendLine "          data,"
        sb.appendLine "          filename: \"${uncapEntityName}-list\""
        sb.appendLine "        });"
        sb.appendLine "        this.downloadLoading = false;"
        sb.appendLine "      });"
        sb.appendLine "    },"
        sb.appendLine "    formatJson(filterVal, jsonData) {"
        sb.appendLine "      return jsonData.map(v =>"
        sb.appendLine "        filterVal.map(j => {"
        sb.appendLine "          if (j === \"timestamp\") {"
        sb.appendLine "            return parseTime(v[j]);"
        sb.appendLine "          } else {"
        sb.appendLine "            return v[j];"
        sb.appendLine "          }"
        sb.appendLine "        })"
        sb.appendLine "      );"
        sb.appendLine "    }"
        sb.appendLine "  }"
        sb.appendLine "}"
        sb.appendLine "</script>"

    }

    void appendDownloadProps(){

        for (int i=0; i<fields.size(); i++){
            def it = fields.get(i)
            if(!BaseUtil.isFk(it)){
                def name = it.name
                if(handler.config.commonProp.contains(name) || handler.config.showProp.contains(name)){
                    continue
                }
                sb.appendLine "          \"$name\""
                if(i!=fields.size()-1) {
                    sb.append ","
                }
            }
        }
    }
    void appendFkIdNotNull(temp){
        def tempObj = temp == null? 'this.temp' : temp
        if(hasOne){
            def arr = []
            fks.each{
                def fkType = BaseUtil.getFkType(it)
                fkType = BaseUtil.specailHandle(fkType)
                def name = StringUtils.uncapitalize(fkType)
                arr.add(name)
                sb.appendLine "          let $name = ${tempObj}.${name}.id?Object.assign({'id':${tempObj}.${name}.id}):null;"
            }
            sb.appendLine "          let params = Object.assign({},${tempObj},{${StringUtils.join(arr,',')}})"
        }else{
            sb.appendLine "          let params = ${tempObj}"
        }
    }

    void appendTempObjFk(){
        if(hasOne){
            for (int i=0; i<fks.size(); i++){
                def fkType = BaseUtil.getFkType(fks.get(i))
                fkType = BaseUtil.specailHandle(fkType)
                def prop = StringUtils.uncapitalize(fkType)
                sb.appendLine "        ${prop}:{}"
                if(i!=fks.size()-1) {
                    sb.append ","
                }
            }
        }
    }

    @Override
    void generateJavaDoc() {

    }
}



interface Generator {
    List<Map> generate(DasTable table, File dir)
    void generateImport();
    public void generateBody();
    void generateJavaDoc();

}


handler = new GlobalHanlder(LOG,PROJECT,SELECTION,CLIPBOARD,FILES,SCRIPTS);
BaseUtil.handler = handler

handler.log("start generate")
handler.generate()
handler.log("finish generate")
println "===================="



println "===================="





