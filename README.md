jedis-x
=======

The extension of [Jedis](https://github.com/xetorthio/jedis) framework.<br>
[Jedis 源码剖析笔记](https://github.com/EdwardLee03/jedis-sr)

Reference:<br>
[1] [Redis](http://redis.io)<br>
[2] [Redis - @江南白衣](https://github.com/springside/springside4/wiki/Redis)

Change log:
# 2014.11.06
  [Redis集群] 基于Jedis定制实现的"Redis服务器异常(宕机)时自动摘除，恢复正常时自动添加"的功能
  [Redis集群] 基于Spring工厂Bean实现的"自定义数据分片的Jedis连接池"工厂
  [Redis集群] 增加Redis服务定义及基于Jedis的自定义服务实现类
  [Memcached集群] 增加基于Spy Memcached的缓存服务及其实现
  [缓存迁移] 增加Redis缓存适配器和缓存迁移服务及其实现
