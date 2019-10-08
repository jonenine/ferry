package mx.utils;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.JsonValueProcessor;
import net.sf.json.util.JSONUtils;

/**
 * 改进的jso类,有空还是看一下jsonlib
 * 以此类形成一种比较灵活的动态数据模型
 */
public class Json {
	
	public static enum TYPE{
		//对象类型
		object,
		//数组类型
		array,
		//其他基本类型都为字符串类型,由业务层去打理
		string,
		//null
		nvl
	}
	
	//内部封装值
	protected JSON json;
	/**
	 * 值---这个值实际上是和json互补的,当表达一个普通的值时json为null,当可以按照json解析时,strValue为null
	 */
	protected String strValue;
	
	/**
	 *  类型
	 */
	private TYPE type;
	public TYPE getType() {
		return type;
	}
	
	//当是对象或数组类型时,可判断是否empty
	public Boolean isEmpty(){
		if(this.type.equals(TYPE.object) || this.type.equals(TYPE.array)) return json.isEmpty();
		//返回null,表示不支持这种类型
		return null;
	}
	
	private static final SimpleDateFormat dateFm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	{
		//要么将时区设置为标准格林威治时区,并调整时间,要么在下面修改为东8区,麻烦
		//dateFm.setTimeZone(TimeZone.getTimeZone("GMT+8"));
	}
	private static final JsonConfig defaultJC=new JsonConfig();
	private static final JsonValueProcessor defaultDateValueProcessor=new JsonValueProcessor(){
		/**
		 * 当作为数组或集合的元素时,应如何解析
		 */
		public Object processArrayValue(Object arg0, JsonConfig arg1) {
			Timestamp timeStamp = new Timestamp(((java.util.Date)arg0).getTime());
			return dateFm.format(timeStamp);
		}
		/**
		 * 当作为对象的属性时应该如何解析
		 */
		public Object processObjectValue(String arg0, Object arg1, JsonConfig arg2) {
			if(arg1==null) return "";
			Timestamp timeStamp = new Timestamp(((java.util.Date)arg1).getTime());
			return dateFm.format(timeStamp);
		}
	};
	
	private static JsonValueProcessor jsonProcessor=new JsonValueProcessor(){
		public Object processArrayValue(Object arg0, JsonConfig arg1) {
			return ((Json)arg0).toString();
		}
		public Object processObjectValue(String arg0, Object arg1, JsonConfig arg2) {
			if(arg1==null) return "";
			return ((Json)arg1).toString();
		}
	};
	
	static {
		/**
		 * 修改时间类型的序列化方式
		 */
		defaultJC.registerJsonValueProcessor(Timestamp.class,defaultDateValueProcessor);
		defaultJC.registerJsonValueProcessor(java.sql.Time.class,     defaultDateValueProcessor);
		defaultJC.registerJsonValueProcessor(java.sql.Date.class,     defaultDateValueProcessor);
		defaultJC.registerJsonValueProcessor(java.util.Date.class,    defaultDateValueProcessor);
		defaultJC.registerJsonValueProcessor(Json.class,    jsonProcessor);
	}
	
	public class NoUse{
		Object object;
		public Object getObject() {return object;}
		public void setObject(Object object) {this.object = object;}
	}

	/**
	 * 注意这个构造器返回类型为nul的json
	 */
	public Json(){
		this(null);
	}
	
	private static final Object object = new Object();
	public static Json createJson(){
		return new Json(object);
	}
	/**
	 * 构造参数要不是json元素,要不是一个java对象.注意同反序列化方法的区别
	 * 约定:字符串"null","undefined" 表达的意义是nvl
	 */
	public Json(Object target){
		//
		if(target==null){
			this.strValue="null";
			this.type=TYPE.nvl;
			this.json=JSONNull.getInstance();
		}else if(JSONObject.class.isAssignableFrom(target.getClass())) {
			this.json =(JSON)target;
			this.type=TYPE.object;
		}else if(JSONArray.class.isAssignableFrom(target.getClass())) {
			this.json =(JSON)target;
			this.type=TYPE.array;
		}else if(JSONNull.class.isAssignableFrom(target.getClass())) {
			this.json =(JSON)target;
			this.type=TYPE.nvl;
		}else if(JSONUtils.isArray(target)){//大概凡是可以被认同为"数组"而被json化的比如set,list,emuration等
			json=JSONArray.fromObject(target,defaultJC);
			this.type=TYPE.array;
		}else if(JSONUtils.isObject(target)){//可以被认为是对象的
			NoUse nu=new NoUse();
			nu.setObject(target);
			Object object=JSONObject.fromObject(nu,defaultJC).get("object");
			if(object instanceof JSONObject){
				json=(JSONObject)object;
				this.type=TYPE.object;
			}else if(object instanceof JSONNull){//字符串"null"属于这里
				this.strValue="null";
				this.type=TYPE.nvl;
				this.json=JSONNull.getInstance();
			}else if(object instanceof String){//时间类型会在这里
				this.strValue=(String)object;
				this.type=TYPE.string;
			}
		}else{
			//其他均按字符串处理
			this.strValue=target.toString();
			this.type=TYPE.string;
		}
	}
	
	/**
	 * 反序列化接口
	 */
	public static Json desierialize(String jsString){
		String _temp="{OO:"+jsString+"}";
		Object target=JSONObject.fromObject(_temp,defaultJC).get("OO");
		return new Json(target);
	}
		
	/**
	 * 如果是数组类型,得到长度
	 * @return
	 */
	public int length(){
		if(this.type.equals(TYPE.array)){
			return ((JSONArray)json).size();
		}
		//返回-1表示不是数组
		return -1;
	}
	
	public List<Json> asList(){
		if(this.type.equals(TYPE.nvl)) return new ArrayList<Json>(0);
		List<Json> re=new LinkedList<Json>();
		if(this.type.equals(TYPE.array)){
			for(int i=0;i<length();i++) re.add(this.find(""+i));
		}else{
			re.add(this);
		}
		return re;
	}
	
	public static interface TravelCallback{
		void process(String id, Json jso, Json parent, int layer);
	}
	
	
	public void travelAllLayer(TravelCallback tCallBack){
		travel(tCallBack,0,-1);
	}
	
	public void travelLimitLayer(TravelCallback tCallBack,int limitLayer){
		travel(tCallBack,0,limitLayer);
	}
	
	/**
	 * 遍历这种树状结构
	 * @param tCallBack
	 * @param layer
	 */
	private synchronized void travel(TravelCallback tCallBack,int layer,int limitLayer){
		if(this.type.equals(TYPE.object)){
			for(String id:((Map<String, Object>)this.json).keySet()){
				Json value=new Json(((Map<String, Object>)this.json).get(id));
				tCallBack.process(id, value ,this ,layer);
				//子一级继续遍历
				if(limitLayer<0 || limitLayer>layer) value.travel(tCallBack,layer+1,limitLayer);
			}
		}else if(this.type.equals(TYPE.array)){
			Iterator it = ((Iterable)this.json).iterator();
			int i=0;
			while(it.hasNext()){
				Json value=new Json(it.next());
				tCallBack.process(""+(i++), value,this,layer);
				//子一级继续遍历
				if(limitLayer<0 || limitLayer>layer) value.travel(tCallBack,layer+1,limitLayer);
			}
		}else if(this.type.equals(TYPE.string) || this.type.equals(TYPE.nvl)){
			return;
		}
	}
	
	/**
	 * 调试用方法
	 */
	public synchronized void debug(){
		this.travel(new TravelCallback(){
			public void process(String id, Json jso, Json parent, int layer) {
				for(int i=0;i<layer;i++) System.out.print("  ");
				System.out.println(id+":("+jso.type+")"+jso.toString());
			}
		},0,-1);
	}
	
	/*----------------作为数据模型,要具备增删改查的能力----------------*/
	
	/**
	 * 原来的get方法改成find,支持深入的查询
	 * 是否应该改成没有这个属性,就返回null
	 * @param idPath id的寻找路径使用.分隔,实际是由属性名称和索引拼起来的
	 * @return
	 */
	public Json find(String idPath){
		//去掉所有空白,转换数组的[i]为对象形式
		idPath=idPath.replaceAll("\\s", "").replaceAll("\\]", "").replaceAll("\\[", ".");
		Json current=this;
		//当前已经解析哦的id,给异常使用
		StringBuffer alreadyIdPath=new StringBuffer();
		String[] ids = idPath.split("\\.");
		for(int i=0,_length=ids.length;i<_length;i++){
			String id=ids[i];
			alreadyIdPath.append((i>0?",":"")+id);
			try {
				if(current.type.equals(TYPE.object))   	 current=new Json(((JSONObject)current.json).get(id));
				else if(current.type.equals(TYPE.array)) current=new Json(((JSONArray)current.json).get(Integer.parseInt(id)));
				else throw new RuntimeException("can't get value from object that not belong to Object or Array type!");
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("can't find property "+id+" in path:"+alreadyIdPath+"\n   current:"+current+"\n this:        "+this);
			}
		}
		
		if(current.getType().equals(TYPE.nvl)) return null;
		return current;
	}
	
	/**
	 * 删除属性,注意不会将作为空集合的父删掉,如果删除属性不存在,将不会抛出异常,但数组索引不能越界
	 * @param idPath
	 * @param propId 要删除的属性索引或名称
	 */
	public synchronized Json remove(String idPath,String propId){
		Json target=find(idPath);
		if(target.type.equals(TYPE.object))  	return new Json(((JSONObject)target.json).remove(propId));
		else if(target.type.equals(TYPE.array))	return new Json(((JSONArray)target.json).remove(Integer.parseInt(propId)));
		else throw new RuntimeException("can't find property "+propId+" in path:"+idPath+"\n    current:"+target+"\n this:        "+this);
		
	}
	
	
	public synchronized void addOrUpadte(String idPath,String propId,Json value){
		Json target=find(idPath);
		if(target.type.equals(TYPE.object)){
			//当是一个可以json化的值时
			if(value.json!=null) ((JSONObject)target.json).element(propId,value.json);
			//当是一个普通值时
			else ((JSONObject)target.json).element(propId,value.strValue);
			
		}else if(target.type.equals(TYPE.array)){
			if(value.json!=null) ((JSONArray)target.json).element(Integer.parseInt(propId),value.json);
			else ((JSONArray)target.json).element(Integer.parseInt(propId),value.strValue);
		}else throw new RuntimeException("can't find property "+propId+" in path:"+idPath+"\n    current:"+target+"\n this:        "+this);
	}
	/**
	 * value可以判断是否为null 
	 */
	public String value(){
		if(type.equals(TYPE.nvl)) return null;
		if(strValue!=null && !strValue.equals("undefined")) return strValue;
		else if(json!=null)	return json.toString();
		//当strValue为undefined时
		return null;
	}
	
	public String toString(){
		String value=value();
		return value==null?"null":value;
	}
	
	
	public static void main(String[] args){
		/*
		Json aaa=new Json().desierialize("[1,2,3,4]");
		aaa.addOrUpadte(".","4", new Json("bbbbb"));
		Json bbb=new Json(aaa.value());
		Json ccc=new Json().desierialize(bbb.value());
		ccc.addOrUpadte(".","5", new Json("fff"));
		System.out.println(ccc.value());
		System.out.println(new Json(new java.util.Date()).value());
		String[] aa= new String[]{"上海\"大白兔\"食品有限公司"}; 
		System.out.println(new Json(aa).value());
	*/
		Json jj = Json.desierialize("{aaa_ff:111,'bbb':{s:1,b:2}}");
		jj.travelAllLayer(new TravelCallback(){
			public void process(String id, Json jso, Json parent, int layer) {
				System.out.println("属性名:"+id+":属性值"+jso.toString()+",层次:"+layer);
			}
		});
		try {
			Random r = new Random();
			File tempDir = null;
			do{
				String random =  (r.nextDouble()+"_"+System.currentTimeMillis()).substring(2);
				String temp = System.getProperty("java.io.tmpdir")+"hadoop_piece\\inpurFormatParams\\"+random;
				tempDir = new File(temp);
			}while(tempDir.exists());
			tempDir.mkdirs();
			
			File tempFile = new File(tempDir.getPath()+"\\conf");
			tempFile.createNewFile();
			System.out.println(tempFile.getPath());
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}


