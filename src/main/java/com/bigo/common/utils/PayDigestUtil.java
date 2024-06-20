package com.bigo.common.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @Description:
 * @author dingzhiwei jmdhappy@126.com
 * @date 2017-07-05
 * @version V1.0
 * @Copyright: www.xxpay.org
 */
@Slf4j
public class PayDigestUtil {


	private static String encodingCharset = "UTF-8";
	
	/**
	 * @param aValue
	 * @param aKey
	 * @return
	 */
	public static String hmacSign(String aValue, String aKey) {
		byte k_ipad[] = new byte[64];
		byte k_opad[] = new byte[64];
		byte keyb[];
		byte value[];
		try {
			keyb = aKey.getBytes(encodingCharset);
			value = aValue.getBytes(encodingCharset);
		} catch (UnsupportedEncodingException e) {
			keyb = aKey.getBytes();
			value = aValue.getBytes();
		}

		Arrays.fill(k_ipad, keyb.length, 64, (byte) 54);
		Arrays.fill(k_opad, keyb.length, 64, (byte) 92);
		for (int i = 0; i < keyb.length; i++) {
			k_ipad[i] = (byte) (keyb[i] ^ 0x36);
			k_opad[i] = (byte) (keyb[i] ^ 0x5c);
		}

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {

			return null;
		}
		md.update(k_ipad);
		md.update(value);
		byte dg[] = md.digest();
		md.reset();
		md.update(k_opad);
		md.update(dg, 0, 16);
		dg = md.digest();
		return toHex(dg);
	}

	public static String toHex(byte input[]) {
		if (input == null)
			return null;
		StringBuffer output = new StringBuffer(input.length * 2);
		for (int i = 0; i < input.length; i++) {
			int current = input[i] & 0xff;
			if (current < 16)
				output.append("0");
			output.append(Integer.toString(current, 16));
		}

		return output.toString();
	}

	/**
	 * 
	 * @param args
	 * @param key
	 * @return
	 */
	public static String getHmac(String[] args, String key) {
		if (args == null || args.length == 0) {
			return (null);
		}
		StringBuffer str = new StringBuffer();
		for (int i = 0; i < args.length; i++) {
			str.append(args[i]);
		}
		return (hmacSign(str.toString(), key));
	}

	/**
	 * @param aValue
	 * @return
	 */
	public static String digest(String aValue) {
		aValue = aValue.trim();
		byte value[];
		try {
			value = aValue.getBytes(encodingCharset);
		} catch (UnsupportedEncodingException e) {
			value = aValue.getBytes();
		}
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		return toHex(md.digest(value));

	}
	
	public static String md5(String value, String charset) {
		MessageDigest md = null;
		try {
			byte[] data = value.getBytes(charset);
			md = MessageDigest.getInstance("MD5");
			byte[] digestData = md.digest(data);
			return toHex(digestData);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getSign(Object o, String key) throws IllegalAccessException {
		if(o instanceof Map) {
			return getSign((Map<String, Object>)o, key);
		}
		ArrayList<String> list = new ArrayList<String>();
		Class cls = o.getClass();
		Field[] fields = cls.getDeclaredFields();
		for (Field f : fields) {
			f.setAccessible(true);
			if (f.get(o) != null && f.get(o) != "") {
				list.add(f.getName() + "=" + f.get(o) + "&");
			}
		}
		int size = list.size();
		String [] arrayToSort = list.toArray(new String[size]);
		Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < size; i ++) {
			sb.append(arrayToSort[i]);
		}
		String result = sb.toString();
		result += "key=" + key;
		log.info("Sign Before MD5:" + result);
		result = md5(result, encodingCharset).toUpperCase();
		log.info("Sign Result:" + result);
		return result;
	}

	public static String getSortJson(JSONObject obj){
		SortedMap map = new TreeMap();
		Set<String> keySet = obj.keySet();
		Iterator<String> it = keySet.iterator();
		while (it.hasNext()) {
			String key = it.next().toString();
			Object vlaue = obj.get(key);
			map.put(key, vlaue);
		}
		return JSONObject.toJSONString(map);
	}

	public static String getSign(Map<String,Object> map, String key){
		ArrayList<String> list = new ArrayList<String>();
		for(Map.Entry<String,Object> entry:map.entrySet()){
			if(null != entry.getValue() && !"".equals(entry.getValue())){
				if(entry.getValue() instanceof JSONObject) {
					list.add(entry.getKey() + "=" + getSortJson((JSONObject) entry.getValue()) + "&");
				}else {
					list.add(entry.getKey() + "=" + entry.getValue() + "&");
				}
			}
		}
		int size = list.size();
		String [] arrayToSort = list.toArray(new String[size]);
		Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < size; i ++) {
			sb.append(arrayToSort[i]);
		}
		String result = sb.toString();
		result += "key=" + key;
		log.info("Sign Before MD5:" + result);
		result = md5(result, encodingCharset).toUpperCase();
		log.info("Sign Result:" + result);
		return result;
	}

	public static String getSortParam(Map<String,Object> map){
		ArrayList<String> list = new ArrayList<String>();
		for(Map.Entry<String,Object> entry:map.entrySet()){
			if(null != entry.getValue() && !"".equals(entry.getValue())){
				if(entry.getValue() instanceof JSONObject) {
					list.add(entry.getKey() + "=" + getSortJson((JSONObject) entry.getValue()) + "&");
				}else {
					list.add(entry.getKey() + "=" + entry.getValue() + "&");
				}
			}
		}
		int size = list.size();
		String [] arrayToSort = list.toArray(new String[size]);
		Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < size; i ++) {
			sb.append(arrayToSort[i]);
		}
		String result = sb.toString();
		return result;
	}


	public static String getSign2(Map<String,Object> map, String key){
		ArrayList<String> list = new ArrayList<String>();
		for(Map.Entry<String,Object> entry:map.entrySet()){
			if(null != entry.getValue() && !"".equals(entry.getValue())){
				if(entry.getValue() instanceof JSONObject) {
					list.add(entry.getKey() + "=" + getSortJson((JSONObject) entry.getValue()) + "&");
				}else {
					list.add(entry.getKey() + "=" + entry.getValue() + "&");
				}
			}
		}
		int size = list.size();
		String [] arrayToSort = list.toArray(new String[size]);
		Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < size; i ++) {
			sb.append(arrayToSort[i]);
		}
		String result = sb.toString();
		result = result.substring(0, result.length()-1) + key;
		log.info("Sign Before MD5:" + result);
		result = md5(result, encodingCharset);
		log.info("Sign Result:" + result);
		return result;
	}

	/**
	 *
	 * @param map
	 * @param key
	 * @param notContains 不包含的签名字段
     * @return
     */
	public static String getSign(Map<String,Object> map, String key, String... notContains){
		Map<String,Object> newMap = new HashMap<String,Object>();
		for(Map.Entry<String,Object> entry:map.entrySet()){
			boolean isContain = false;
			for(int i=0; i<notContains.length; i++) {
				if(entry.getKey().equals(notContains[i])) {
					isContain = true;
					break;
				}
			}
			if(!isContain) {
				newMap.put(entry.getKey(), entry.getValue());
			}
		}
		return getSign(newMap, key);
	}

	public static String createSortParam(JSONObject paramJson, String... notContains){
		Map<String,Object> newMap = new HashMap<String,Object>();
		for(Map.Entry<String,Object> entry : paramJson.entrySet()){
			boolean isContain = false;
			for(int i=0; i<notContains.length; i++) {
				if(entry.getKey().equals(notContains[i])) {
					isContain = true;
					break;
				}
			}
			if(!isContain) {
				newMap.put(entry.getKey(), entry.getValue());
			}
		}
		return createLinkString(newMap);
	}

	public static String createLinkString(Map<String, Object> params) {
		List<String> keys = new ArrayList<>(params.keySet());
		Collections.sort(keys);
		StringBuilder prestr = new StringBuilder();
		for (int i = 0;i < keys.size();i++) {
			String key = keys.get(i);
			String value = String.valueOf(params.get(key));
			if(value.equals("null")){
				value = "";
			}
			if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
				prestr.append(key).append("=").append(value);
			} else {
				prestr.append(key).append("=").append(value).append("&");
			}
		}
		return prestr.toString();
	}

	public static String HmacSHA256(Map<String, Object> params, String privateKey)  {
		String encodeStr16 = null;
		try {

			List<String> keys = new ArrayList<>(params.keySet());
			Collections.sort(keys);
			StringBuilder prestr = new StringBuilder();
			for (int i = 0;i < keys.size();i++) {
				String key = keys.get(i);
				String value = String.valueOf(params.get(key));
				if(value.equals("null")){
					value = "";
				}
				if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
					prestr.append(key).append("=").append(value);
				} else {
					prestr.append(key).append("=").append(value).append("&");
				}
			}

			String result = prestr.toString();
//			result = result.substring(0, result.length() - 1);
			log.info("Sign Before SHA256:" + result);
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKey = new SecretKeySpec(privateKey.getBytes("utf-8"), "HmacSHA256");
			sha256_HMAC.init(secretKey);
			byte[] hash = sha256_HMAC.doFinal(result.getBytes("utf-8"));
			encodeStr16=byte2Hex(hash);
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return encodeStr16;
	}


	/**
	 * 将byte转为16进制
	 *
	 * @param bytes
	 * @return
	 */
	private static String byte2Hex(byte[] bytes) {
		StringBuffer stringBuffer = new StringBuffer();
		String temp = null;
		for (int i = 0; i < bytes.length; i++) {
			temp = Integer.toHexString(bytes[i] & 0xFF);
			if (temp.length() == 1) {
				//1得到一位的进行补0操作
				stringBuffer.append("0");
			}
			stringBuffer.append(temp);
		}
		return stringBuffer.toString();
	}




}
