package com.igoosd.util;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

	@Resource(name = "redisTemplate")
	private RedisTemplate redisTemplate;

	public void set(final byte[] key, final byte[] value, final long liveTime) {
		redisTemplate.execute(new RedisCallback<Object>() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				connection.set(key, value);
				if (liveTime > 0) {
					connection.expire(key, liveTime);
				}
				return 1L;
		}
		});
	}

	public Boolean setnx(final String key, final String value, final long liveTime) {
		return (Boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				Boolean res = connection.setNX(key.getBytes(), value.getBytes());
				if (liveTime > 0) {
					connection.expire(key.getBytes(), liveTime);
				}
				return res;
		}
		});
	}

	public void set(String key, String value, long liveTime) {
		set(key.getBytes(), value.getBytes(), liveTime);
	}

	public Object get(final String key) {
		return redisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection conn) {
				byte[] bvalue = conn.get(key.getBytes());
				if (null == bvalue) {
					return null;
				} else {
					return redisTemplate.getStringSerializer().deserialize(bvalue);
				}
			}
		});
	}

	public void hSet(final String key, final String field, final String value) {
		redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection conn) throws DataAccessException {
				conn.hSet(key.getBytes(), field.getBytes(), value.getBytes());
				return null;
			}
		});
	}

	public Object hGet(final String key, final String field) {
		return redisTemplate.execute((RedisCallback<Object>) conn -> {
            byte[] bvalue = conn.hGet(key.getBytes(), field.getBytes());
            if (null == bvalue) {
                return null;
            } else {
                return redisTemplate.getStringSerializer().deserialize(bvalue);
            }
        });
	}

	// 往 Redis 中写入 Map 类型的数据
	public void hMSet(final String key, final Map<String, Object> map) {
		redisTemplate.execute((RedisCallback<Object>) connection -> {
            Map byteMap = new HashMap(map.size());
            for (final Map.Entry entry : map.entrySet()) {
                byte[] mapKey = serializeValue(entry.getKey());
                byte[] mapValue = serializeValue(String.valueOf(entry.getValue()));
                byteMap.put(mapKey, mapValue);
            }
            connection.hMSet(serializeKey(key), byteMap);
            byteMap = null; // 释放内存中的map
            return byteMap;
        });
	}

	// 从 Redis 中获取 Map 类型中制定 key 的数据（如果 field 为空的话，则取整个 Map）
	public Object hMGet(final String key, final Object field) {
		return redisTemplate.execute((RedisCallback) connection -> {
			if (null == field || "".equals(String.valueOf(field))) {
				return hGetAll(key);
			} else {
				List<byte[]> list = connection.hMGet(serializeKey(key), serializeValue(field));
				if (0 < list.size()) {
					return deserializeValue(list.get(0));
				} else {
					return null;
				}
			}
        });
	}

	public Map<String, Object> hGetAll(final String key) {
		return (Map) redisTemplate.execute((RedisCallback) connection -> {
			Map<byte[], byte[]> map = connection.hGetAll(serializeKey(key));
			Map<String, Object> objectMap = new HashMap<>();
			for (final Map.Entry entry : map.entrySet()) {
				Object mapKey = deserializeValue((byte[]) entry.getKey());
				Object mapValue = deserializeValue((byte[]) entry.getValue());
				objectMap.put(String.valueOf(mapKey), mapValue);
			}
			return objectMap;
		});
	}

	// hdel 删除 Map 中 制定的 key


	public void rPush(final String key, final String value) {
		redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection conn) throws DataAccessException {
				conn.rPush(key.getBytes(), value.getBytes());
				return null;
			}
		});
	}

	// 设置某一key的超时时间，单位秒
	public void expired(String key, long timeout) {
		redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
	}

	public void del(final String key) {
		redisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection conn) {
				return conn.del(key.getBytes());
			}
		});
	}

	/**
	 * key以秒为单位,返回给定 key 的剩余生存时间
	 *
	 * @param key
	 */
	public Long ttl(final String key) {
		return (Long) redisTemplate.execute(new RedisCallback<Long>() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.ttl(key.getBytes());
			}
		});
	}

	/**
	 * 对一个key的value加1
	 *
	 * @param key
	 */
	public Long incr(final String key) {
		return (Long) redisTemplate.execute(new RedisCallback<Long>() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.incr(key.getBytes());
			}
		});
	}

	public Long getTtl(final String key) {
		return (Long) redisTemplate.execute(new RedisCallback<Object>() {
			public Long doInRedis(RedisConnection conn) {
				Long value = conn.ttl(key.getBytes());
				return value;
			}
		});
	}

	public Object sAdd(final String key, final String value) {
		return redisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection conn) throws DataAccessException {
				return conn.sAdd(key.getBytes(), value.getBytes());
			}
		});
	}

	public Object sIsMember(final String key, final String value) {
		return redisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection conn) throws DataAccessException {
				return conn.sIsMember(key.getBytes(), value.getBytes());
			}
		});
	}

	private byte[] serializeKey(final String key) {
		return redisTemplate.getStringSerializer().serialize(key);
	}

	private byte[] serializeValue(final Object value) {
		return redisTemplate.getValueSerializer().serialize(value);
	}

	protected Object deserializeValue(final byte[] value) {
		return redisTemplate.getValueSerializer().deserialize(value);
	}

}
