package mx.utils;

import io.netty.buffer.ByteBuf;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.lang.StringUtils;

public class ByteUtils {
	
	/**
	 * 此方法不改变buf的writeIndex和readIndex 
	 */
	public static byte[] getBytes(ByteBuf buf){
		int size = buf.writerIndex();
		byte[] bytes = new byte[size];
		buf.getBytes(0, bytes);
		
		return bytes;
	}
	
	/**
	 * 紧凑型 
	 */
	public static String toCompactHexString(byte[] bytes){
		StringBuffer sb = new StringBuffer();
		for(int i= 0;i<bytes.length;i++){
			String h = Integer.toString(bytes[i]&0xff,16).toUpperCase();
			h = h.length()>1?h:("0"+h);
			sb.append(h);
		}
		return sb.toString();
	}
	
	public static String toHexString(Object obj){
		if(obj == null) return "null";
		
		if(obj.getClass().isArray()){
			byte[] bytes = (byte[]) obj;
			StringBuffer sb = new StringBuffer();
			for(int i= 0;i<bytes.length;i++){
				String h = Integer.toString(bytes[i]&0xff,16).toUpperCase();
				h=h.length()>1?h:("0"+h);
				sb.append(h+" ");
			}
			return sb.toString();
		}else if(obj instanceof Byte){
			return toHexString(new byte[]{(Byte)obj});
		}else if(obj instanceof ByteBuf){
			return toHexString(getBytes((ByteBuf)obj));
		}
		
		throw new RuntimeException("can't get hex for class "+obj.getClass());
	}
	
	public static byte[] toBytes(String hexs){
		String[] hexes = hexs.split("\\s+");
		
		byte[] re = new byte[hexes.length];
		for(int i=0;i<re.length;i++){
			re[i] = (byte) Integer.parseInt(hexes[i],16);
		}
		
		return re;
	}
	
	public static byte[] toBytesCompact(String hexs){
		
		int l = hexs.length()/2;
		byte[] re = new byte[l];
		for(int i=0;i<l;i++){
			re[i] = (byte) Integer.parseInt(hexs.substring(i*2, (i+1)*2),16);
		}
		
		return re;
	}
	
	public static String toBinaryString(byte[] bs){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<bs.length;i++){
			String bStr = Integer.toString(bs[i]&0xff, 2);
			bStr = StringUtils.leftPad(bStr, 9-bStr.length(), "0");
			sb.append(bStr);
		}
		return sb.toString();
	}
 	
	
	/**
	 * 注意这个方法是大端序的
	 * @param bytes
	 * @return
	 */
	public static long bytesToLong(byte[] bytes){
		long l =0 ;
		for(int i=0;i<bytes.length;i++){
			l<<=8;
			l+=(bytes[i]&0xff);
		}
		return l;
	}
	
	
	public static int bytesToInt(byte[] bytes){
		int l =0 ;
		for(int i=0;i<bytes.length;i++){
			l<<=8;
			l+=(bytes[i]&0xff);
		}
		return l;
	}
	
	
	
	
	/**
	 * 转成小端字节序的bcd码
	 * 地址域是小端字节序,规约里面抄通信地址是大端的
	 */
	public static String bytesToBCD_LTE(byte[] bytes){
		StringBuilder sb = new StringBuilder();
		for(int i=bytes.length-1;i>=0;i--){
			sb.append((bytes[i]&0xff)>>>4);
			int low = bytes[i]&0xf;
			if(low<10) sb.append(low);
		}
		
		return sb.toString();
	}
	
	public static String bytesToBCD(byte[] bytes){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<bytes.length;i++){
			sb.append((bytes[i]&0xff)>>>4);
			int low = bytes[i]&0xf;
			if(low<10) sb.append(low);
		}
		
		return sb.toString();
	}
	
	/**
	 * bytesToBCD_LTE方法的反方法
	 * 地址域是小端字节序
	 */
	public static byte[] bCD_LTEToBytes(String bcd){
		char[] chs = bcd.toCharArray();
		
		byte[] re = new byte[chs.length/2+chs.length%2];
		int j=0;
		for(int i=chs.length-1;i>=0;i--){
			int low  = chs[i]-48;
			int high;
			if(--i<0){
				high = low;
				low = 15;
			}else{
				high = chs[i]-48;
			}
			
			re[j++] = (byte) ((high<<4)+low);
		}
		return re;
	}
	
	public static byte[] bCDToBytes(String bcd){
		char[] chs = bcd.toCharArray();
		
		byte[] re = new byte[chs.length/2+chs.length%2];
		int j=0;
		for(int i=0;i<chs.length;i++){
			int high  = chs[i]-48;
			int low;
			if(++i<0){
				low = high;
				high = 15;
			}else{
				low = chs[i]-48;
			}
			
			re[j++] = (byte) ((high<<4)+low);
		}
		return re;
	}
	
	
	public static byte[] reverse(byte[] array){
		int l = array.length;
		byte[] _array = new byte[l];
		for(int i=0;i<l;i++) _array[i] = array[l-i-1];
		return _array;
	}
	
	public static String reverse(String str){
		char[] chs = str.toCharArray();
		int l = chs.length;
		char[] _chs = new char[l];
		for(int i=0;i<l;i++) _chs[i] = chs[l-i-1];
		
		return new String(_chs);
	}
	
	/**
	 * @param source
	 * @param start
	 * @param end 同样不包括end
	 * @return
	 */
	public static byte[] subBytes(byte[] source,int start,int end){
		int l = end-start;
		byte[] target = new byte[l];
		System.arraycopy(source, start, target, 0, l);
		return target;
	}
	
	
	public static byte[] insertBytes(byte[] source,int index,byte[] toInsert){
		int l = source.length+toInsert.length;
		byte[] target = new byte[l];
		System.arraycopy(source, 0, target, 0, index);
		System.arraycopy(toInsert, 0, target, index, toInsert.length);
		System.arraycopy(source, index, target, index+toInsert.length, source.length-index);
		return target;
	}
	
	
	public static byte[] subBytes(byte[] source,int start){
		return subBytes(source,start,source.length);
	}
	
	
	
	public static byte[] concat(byte[]... arrays){
		int length = 0;
		for(int i=0;i<arrays.length;i++){
			length+=arrays[i].length;
		}
		byte[] array = new byte[length];
		
		int lengthAlready = 0;
		for(int i=0;i<arrays.length;i++){
			System.arraycopy(arrays[i], 0, array, lengthAlready, arrays[i].length);
			lengthAlready +=arrays[i].length;
		}
		
		return array;
	}
	
	public static boolean equals(byte[] source, int startIndex, byte[] target){
		int i = 0;
		for(;i<target.length;i++){
			if(source[startIndex+i]!=target[i]){
				return false;
			}
		}
		
		return true;
	}
	
	public static byte[] replaceFirst(byte[] source, int fromIndex,byte[] old,byte[] replace){
		for(int i=fromIndex;i<source.length;i++){
			if(equals(source,i,old)){
				byte[] left  = subBytes(source,0,i);
				byte[] right = subBytes(source,i+old.length);
				return concat(left,replace,right);
			}
		}
		
		return source;
	}
	
	public static byte[] replaceFirst(byte[] source,byte[] old,byte[] replace){
		return replaceFirst(source,0,old,replace);
	}
	
	public static void main(String[] args){
		byte[] bs = toBytes("00 11 22 55 66");
		byte[] bs1 = toBytes("55");
		byte[] bs2 = toBytes("33 44");
		
		byte[] bs3 = replaceFirst(bs,bs1,bs2);
		System.err.println(toHexString(bs3));
		System.err.println(equals(bs,1,bs1));
	}
	
	
	
	/**
	 * 供转换用
	 */
	public static Calendar toDate(Integer year,Integer month,Integer day_of_month,Integer hour,Integer minute,Integer second,Integer milliseconds){
		Calendar cal=new GregorianCalendar();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day_of_month);
		if(hour!=null)   cal.set(Calendar.HOUR_OF_DAY, hour);
		if(minute!=null) cal.set(Calendar.MINUTE, minute);
		if(second!=null) cal.set(Calendar.SECOND, second);
		if(milliseconds!=null) cal.set(Calendar.MILLISECOND, milliseconds);
		
		return cal;
	}
	
	
}
