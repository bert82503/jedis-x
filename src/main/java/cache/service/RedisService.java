/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cache.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 封装<a href="http://redis.io">Redis</a>客户端，并增加开关功能和资源关闭功能。
 * <p>
 * 【XML配置示例】
 * 
 * <pre>
 * {@literal
 * <bean id="redisService" class="redis.client.jedis.JedisServiceImpl" destroy-method="close">
 * }
 *    &lt;property name="enabled" value="${redis.enabled}" />
 * {@literal
 * </bean>
 * }
 * </pre>
 * 
 * 【参考资料】
 * <ul>
 * <li><a href="http://redis.io/commands">Commands - Redis</a>
 * <li><a href="https://github.com/springside/springside4/wiki/Redis">Redis - @江南白衣</a>
 * </ul>
 * 
 * @author huagang.li 2014年12月12日 上午10:26:14
 */
public interface RedisService extends SwitchService {

    /** 列表或集合的默认最大长度 */
    int DEFAULT_MAX_LENGTH = 3000;

    /** 列表或有序集合的长度阈值 */
    int LENGTH_THRESHOLD   = 50;

    // =======================================================
    // Key (键) - http://redis.io/commands#generic
    // Key不能太长，比如1024字节，但antirez (Redis作者)也不喜欢太短如"u:1000:pwd"，要表达清楚意思才好。
    // 他私人建议用":"分隔域，用"."作为单词间的连接，如"comment:12345:reply.to"。
    // =======================================================
    /**
     * 为给定key设置生存时间。<br>
     * 当key过期后(生存时间为0)，它会被自动删除。在Redis中，带有生存时间的key被称为『易失的』(volatile)。
     * <p>
     * 生存时间可以通过使用DEL命令来删除整个key来移除，或者被SET和GETSET命令覆写。<br>
     * 这意味着，如果一个命令只是修改一个带生存时间的key的值而不是用一个新的key值来代替它的话，那么生存时间不会被改变。<br>
     * 例如，对一个key执行INCR命令，对一个列表进行LPUSH命令，或者对一个哈希表执行HSET命令，这类操作都不会修改key本身的生存时间。
     * <p>
     * 时间复杂度: O(1)<br>
     * EXPIRE key seconds - http://redis.io/commands/expire
     *
     * @param key 键
     * @param seconds 生存时间(秒数)
     * @return 当超时设置成功时，返回1；当key不存在或者不能为key设置生存时间时，返回0。
     */
    int expire(String key, int seconds);

    /**
     * 返回给定key的剩余生存时间(TTL, time to live，以秒为单位)。
     * <p>
     * 时间复杂度: O(1)<br>
     * http://redis.io/commands/ttl
     * 
     * @param key
     * @return 当key不存在时，返回-2； 当key存在但没有设置剩余生存时间时，返回-1； 否则，以秒为单位，返回key的剩余生存时间。
     */
    long ttl(String key);

    /**
     * 删除给定的一个或多个keys。<br>
     * 不存在的key会被忽略。
     * <p>
     * 时间复杂度: O(N)，N为被删除的key的数量<br>
     * 删除单个字符串类型的key，时间复杂度为O(1)。<br>
     * 删除单个列表、集合、有序集合或哈希表类型的key，时间复杂度为O(M)，M为以上数据结构内的元素数量。<br>
     * DEL key [key ...] - http://redis.io/commands/del
     * 
     * @param key 键
     * @return 被删除key的数量；当没有key被删除时，返回0。
     */
    int del(String key);

    // =======================================================
    // String (字符串) - http://redis.io/commands#string
    // 最普通的key-value类型，说是String，其实是任意的byte[]，比如图片，最大512M。
    // 所有常用命令的复杂度都是O(1)，普通的Get/Set方法，可以用来做Cache，存Session，为了简化架构甚至可以替换掉Memcached。
    // Incr/IncrBy/Decr/DecrBy，可以用来做计数器，做自增序列。
    // SetEx，Set + Expire的简便写法，p字头版本以毫秒为单位。
    // GetSet，设置新值，返回旧值。比如一个按小时计算的计数器，可以用GetSet获取计数并重置为0。这种指令在服务端做起来是举手之劳，客户端便方便很多。
    // MGet/MSet，一次get/set多个key。
    // 2.6.12版开始，Set命令已融合了Set/SetNx/SetEx三者，SetNx与SetEx可能会被废弃，这对Master抢注非常有用，不用担心setNx成功后，来不及执行Expire就倒掉了。
    // GetBit/SetBit/BitCount，BitMap的玩法，比如统计今天的独立访问用户数时，每个注册用户都有一个offset，他今天进来的话就把他那个位设为1，用BitCount就可以得出今天的总人数。
    // =======================================================
    /**
     * 返回key所关联的字符串值。<br>
     * 如果key不存在，那么返回特殊值null； 如果key储存的值不是字符串类型，则返回一个错误，因为GET只能用于处理字符串值。
     * <p>
     * 时间复杂度: O(1)<br>
     * GET key - http://redis.io/commands/get
     * 
     * @param key 键
     * @return 当key不存在时，返回null；否则，返回key的值。
     */
    String get(String key);

    /**
     * 将字符串值value关联到key。(key永不过期)<br>
     * 如果key已经持有其他值，SET就覆写旧值，无视类型。<br>
     * 对于某个原本带有生存时间(TTL)的key来说，当SET命令成功在这个key上执行时，这个key原有的TTL将被清除。
     * <p>
     * <h3>可选参数</h3>
     * <hr>
     * 从Redis 2.6.12版本开始，SET命令的行为可以通过一系列参数来修改：
     * <ul>
     * <li>EX second：设置key的过期时间为second秒。SET key value EX second 效果等同于 SETEX key second value
     * <li>PX millisecond：设置键的过期时间为millisecond毫秒。SET key value PX millisecond 效果等同于 PSETEX key millisecond value
     * <li>NX：只在键不存在时，才对键进行设置操作。SET key value NX 效果等同于 SETNX key value
     * <li>XX：只在键已经存在时，才对键进行设置操作。
     * </ul>
     * 注意：因为SET命令可以通过参数来实现和SETNX、SETEX和PSETEX三个命令的效果，所以将来的Redis版本可能会废弃并最终移除SETNX、SETEX和PSETEX这三个命令。
     * <p>
     * 时间复杂度: O(1)<br>
     * SET key value [EX seconds] [PX milliseconds] [NX|XX] - http://redis.io/commands/set
     * 
     * @param key 键
     * @param value 字符串值
     * @return 当设置操作成功时，返回OK；否则，返回空批量回复(null)。因为该方法未使用SET命令的可选参数，所以总是会返回OK，不可能失败。
     */
    String set(String key, String value);

    // // 批量操作
    // /**
    // * 返回所有给定key的值。<br>
    // * 如果在给定的key里面，有某个key不存在，那么这个key返回特殊值null。因此，该命令永不失败。
    // * <p>
    // * 时间复杂度: O(N)，N为检索的keys的数量<br>
    // * MGET key [key ...] - http://redis.io/commands/mget
    // *
    // * @param keys 键列表(<key1, key2, ... , keyN>)
    // * @return 一个包含所有给定key的值的列表。
    // */
    // List<String> mget(String... keys);
    //
    // /**
    // * 同时设置一个或多个key-value对。(所有key都永不过期)<br>
    // * 如果某个给定key已经存在，那么MSET会用新值覆盖原来的旧值，和SET一样。
    // * <p>
    // * MSET是一个原子操作，所有给定key都会在同一时间内被设置。<br>
    // * 某些给定key被更新而另一些给定key没有改变的情况，是不可能发生的。
    // * <p>
    // * Return value: always OK since MSET can't fail. (总是返回OK，因为MSET不可能失败。所以，不需要返回值。)<br>
    // * <p>
    // * 时间复杂度: O(N) where N is the number of keys to set. (N为待设置keys的数量)<br>
    // * MSET key value [key value ...] - http://redis.io/commands/mset
    // *
    // * @param keysvalues "键-值对"列表(<key1, value1, key2, value2, ... , keyN, valueN>)
    // */
    // void mset(String... keysvalues);

    // key 过期
    /**
     * 将字符串值value关联到key，并将key的生存时间设为seconds。<br>
     * 如果key已经存在，SETEX命令将覆写旧值。
     * 
     * <pre>
     * 这个命令等价于执行以下两个命令：
     *      SET mykey value
     *      EXPIRE mykey seconds
     * </pre>
     * 
     * 不同之处是：SETEX是一个原子操作，关联值和设置生存时间两个动作会在同一时间内完成。<br>
     * 该命令在Redis用作缓存时，非常实用。
     * <p>
     * <font color="red"><b>注意：</b></font>生存时间必须大于0；若小于或等于0，请使用{@link #set(String, String)}方法！
     * <p>
     * 时间复杂度: O(1)<br>
     * SETEX key seconds value - http://redis.io/commands/setex
     * 
     * @param key 键
     * @param seconds 生存时间(秒数)
     * @param value 字符串值
     * @return 当设置操作成功时，返回OK；当seconds参数不合法(<= 0)时，返回{@code null}。
     */
    String setex(String key, int seconds, String value);

    // =======================================================
    // List (列表) - http://redis.io/commands#list
    // List是一个双向链表，支持双向的Pop/Push，江湖规矩一般从"左端Push，右端Pop——LPush/RPop"。
    // 还有Blocking的版本BLPop/BRPop，客户端可以阻塞在那直到有消息到来。
    // 所有操作都是O(1)的好孩子，可以当Message Queue来用。
    // 当多个Client并发阻塞等待，有消息入列时谁先被阻塞谁先被服务。任务队列系统Resque是其典型应用。
    // LRange，不同于POP直接弹走元素，只是返回列表内一段下标的元素，是分页的最爱。
    // LTrim，限制List的大小，比如只保留最新的20条消息。
    // =======================================================
    /**
     * 返回列表key的长度。<br>
     * 如果key不存在，则key被解释为一个空列表，返回0。<br>
     * 如果key不是列表类型，则返回一个错误。
     * <p>
     * 时间复杂度: O(1)<br>
     * LLEN key - http://redis.io/commands/llen
     *
     * @param key 键
     * @return 列表key的长度。
     */
    int llen(String key);

    // 江湖规矩一般从"左端Push，右端Pop——LPush/RPop"
    /**
     * 将所有给定value插入到列表key的表头。<br>
     * 如果key不存在，一个空列表会被创建并执行LPUSH操作。当key存在但不是列表类型时，会返回一个错误。
     * <p>
     * 如果有多个value值，那么各个value值按从左到右的顺序依次插入到表头。<br>
     * 例如，对空列表mylist执行命令LPUSH mylist a b c，列表的值将是c b a，这等同于原子性地执行LPUSH mylist a、LPUSH mylist b和LPUSH mylist c三个命令。
     * <p>
     * 时间复杂度: O(1)<br>
     * LPUSH key value [value ...] - http://redis.io/commands/lpush
     * 
     * @param key 键
     * @param values 字符串值列表(<key, value1, value2, ... , valueN>)
     * @return 执行LPUSH命令后，列表的长度。
     */
    int lpush(String key, String... values);

    /**
     * 移除并返回列表key的表尾元素。
     * <p>
     * 时间复杂度: O(1)<br>
     * RPOP key - http://redis.io/commands/rpop
     *
     * @param key 键
     * @return 当列表不为空时，返回列表的表尾元素；当key不存在或列表为空时，返回null。
     */
    String rpop(String key);

    // 范围检索
    // LRange 是分页的最爱
    /**
     * 返回列表key中指定区间内的元素，区间由偏移量start和stop指定。<br>
     * 下标参数start和stop都以0为基底，也就是说，0表示列表的第一个元素，1表示列表的第二个元素，以此类推。<br>
     * 下标参数start和stop也可以是负数，-1表示列表的最后一个元素，-2表示列表的倒数第二个元素，以此类推。
     * <p>
     * <h3>超出范围的下标</h3>
     * <hr>
     * 超出范围的下标值不会引起错误。<br>
     * 如果start下标比列表的最大下标end (LLEN list 减去 1)还要大，那么LRANGE返回一个空列表。<br>
     * 如果stop下标比end下标还要大，Redis将stop的值设置为end。
     * <p>
     * 时间复杂度: O(S+N)，S为偏移量start，N为指定区间内元素的数量<br>
     * LRANGE key start stop - http://redis.io/commands/lrange
     * 
     * @param key 键
     * @param start 起始下标
     * @param stop 结束下标
     * @return 一个列表，包含指定区间内的元素；如果指定区间不包含任何元素，则返回一个空列表。
     */
    List<String> lrange(String key, int start, int stop);

    // LTrim，限制List的大小（比如只保留最新的20条消息）
    /**
     * 对一个列表进行修剪，就是说，让列表只保留指定区间内的元素，不在指定区间内的元素都将被删除。<br>
     * 下标参数start和stop都是以0为基底，也就是说，0表示列表的第一个元素，1表示列表的第二个元素，以此类推。
     * <p>
     * 例如：执行命令LTRIM foobar 0 2，表示只保留列表foobar的前三个元素，其余元素全部删除。
     * <p>
     * 下标参数start和stop也可以是负数，-1表示列表的最后一个元素，-2表示列表的倒数第二个元素，以此类推。<br>
     * <font color="red">注意：</font>表头、表尾下标值表示的区别！
     * <p>
     * 超出范围的下标值不会引起错误：如果start下标比列表的最大下标end (LLEN list减去1)还要大，或者start >
     * stop，LTRIM返回一个空列表(因为LTRIM已经将整个列表清空)。如果stop下标比end下标还要大，Redis将stop的值设置为end。
     * <p>
     * LTRIM命令通常和LPUSH或RPUSH命令配合使用。例如：
     * 
     * <pre>
     * LPUSH mylist someelement
     * LTRIM mylist 0 99
     * </pre>
     * 
     * 这个例子模拟了一个日志程序，每次将最新日志newest_log放到log列表中，并且只保留最新的100项。<br>
     * 值得注意的是：当这样使用LTRIM命令时，时间复杂度是O(1)，因为平均情况下每次只有一个元素被移除。
     * <p>
     * 时间复杂度: O(N)，N为被移除的元素的数量<br>
     * LTRIM key start stop - http://redis.io/commands/ltrim
     *
     * @param key 键
     * @param start 起始下标
     * @param stop 结束下标
     * @return 当命令执行成功时，返回OK。
     */
    String ltrim(String key, int start, int stop);

    // =======================================================
    // Sorted Set (有序集合) - http://redis.io/commands#sorted_set
    // 有序集，元素放入集合时还要提供该元素的分数，默认是从小到大排列。
    // ZRange/ZRevRange，按排名的上下限返回元素，正数与倒数。
    // ZRangeByScore/ZRevRangeByScore，按分数的上下限返回元素，正数与倒数。
    // ZRemRangeByRank/ZRemRangeByScore，按排名/按分数的上下限删除元素。
    // ZCount，统计分数上下限之间的元素个数。
    // ZAdd(Add)/ZRem(Remove)/ZCard(Count)，ZInsertStore(交集)/ZUnionStore(并集)，Set操作，与正牌Set相比，少了IsMember和差集运算。
    // ZAdd/ZRem是O(log(N))，ZRangeByScore/ZRemRangeByScore是O(log(N)+M)，N是Set大小，M是结果/操作元素的个数。
    // 可见，原本可能很大的N被很关键的Log了一下，1000万大小的Set，复杂度也只是几十不到。当然，如果一次命中很多元素，M很大那谁也没办法了。
    // =======================================================
    /**
     * 将所有给定member元素及其score值加入到有序集key中。(默认最大长度为{@link #DEFAULT_MAX_LENGTH})<br>
     * 如果有序集的长度超过"默认最大长度({@link #DEFAULT_MAX_LENGTH})"，则会自动进行"异步缩容"操作，删除那些最老的元素。<br>
     * 如果某个member已经是有序集的成员，那么更新这个member的score值，并通过重新插入这个member元素，来保证该member在正确的位置上。
     * <p>
     * 如果key不存在，则创建一个空的有序集并执行ZADD操作。当key存在但不是有序集类型时，返回一个错误。
     * <p>
     * 时间复杂度: O(log(N))，N是有序集的基数，M为成功添加的新成员的数量<br>
     * ZADD key score member [score member ...] - http://redis.io/commands/zadd
     * 
     * @param key 键
     * @param score 元素的分数
     * @param member 元素
     * @return 被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员。
     */
    int zadd(String key, double score, String member);

    /**
     * 将所有给定member元素及其score值加入到有序集key中。<br>
     * 如果有序集的长度超过"最大长度阈值({@code maxLength})"参数，则会自动进行"异步缩容"操作，删除那些最老的元素。
     * <p>
     * 见{@link #zadd(String, double, String)}文档注释。
     * 
     * @param key 键
     * @param score 元素的分数
     * @param member 元素
     * @param maxLength 最大长度阈值
     * @return 被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员。
     */
    int zadd(String key, double score, String member, int maxLength);

    // 批量增加
    /**
     * 将"member元素及其score值"加入到有序集key中。(默认最大长度为{@link #DEFAULT_MAX_LENGTH})<br>
     * 如果有序集的长度超过"默认最大长度({@link #DEFAULT_MAX_LENGTH})"，则会自动进行"异步缩容"操作，删除那些最老的元素。
     * <p>
     * 见{@link #zadd(String, double, String)}文档注释。
     * 
     * @param key 键
     * @param scoreMembers {@literal <元素, 元素的分数>}的映射表
     * @return 被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员。
     */
    int zadd(String key, Map<String, Double> scoreMembers);

    /**
     * 将"member元素及其score值的映射表"加入到有序集key中。<br>
     * 如果有序集的长度超过"最大长度阈值({@code maxLength})"参数，则会自动进行"异步缩容"操作，删除那些最老的元素。
     * <p>
     * 见{@link #zadd(String, double, String)}文档注释。
     * 
     * @param key 键
     * @param scoreMembers {@literal <元素, 元素的分数>}的映射表
     * @param maxLength 最大长度阈值
     * @return 被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员。
     */
    int zadd(String key, Map<String, Double> scoreMembers, int maxLength);

    /**
     * 返回有序集key中，指定区间内的成员。
     * <p>
     * 其中成员按score值递增(从小到大)来排序，具有相同score值的成员按字典序来排列。
     * <p>
     * 时间复杂度: O(log(N)+M)，N为有序集的基数，而M为结果集的基数<br>
     * ZRANGE key start stop [WITHSCORES] - http://redis.io/commands/zrange
     * 
     * @param key
     * @param start
     * @param stop
     * @return 指定区间内，带有score值(可选)的有序集成员的列表。
     */
    Set<String> zrange(String key, int start, int stop);

    /**
     * 返回有序集key中，指定区间内的成员。
     * <p>
     * 其中成员的位置按score值递减(从大到小)来排列，具有相同score值的成员按字典序的反序排列。
     * 
     * @param key
     * @param start
     * @param stop
     * @return
     */
    Set<String> zrevrange(String key, int start, int stop);

    // 范围检索
    /**
     * 返回有序集key中，所有score值介于min和max之间(包括等于min或max)的成员。<br>
     * 有序集成员按 score 值递增(从小到大)次序排列。
     * <p>
     * 具有相同score值的成员按字典序来排列(该属性是有序集提供的，不需要额外的计算)。
     * <p>
     * 时间复杂度: O(log(N)+M)，N为有序集的基数，M为结果集的基数<br>
     * ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count] - http://redis.io/commands/zrangebyscore
     * 
     * @param key 键
     * @param min 检索的最小分数
     * @param max 检索的最大分数
     * @return 指定区间内，带有score值(可选)的有序集成员的列表。
     */
    Set<String> zrangeByScore(String key, double min, double max);

    /**
     * 返回有序集key中，所有score值介于min和max之间(包括等于min或max)的成员。
     * <p>
     * 见{@link #zrangeByScore(String, double, double)}文档注释。
     * 
     * @param key 键
     * @param min 检索的最小分数
     * @param max 检索的最大分数
     * @param offset 返回列表的偏移量
     * @param count 返回列表的最大元素个数
     * @return 指定区间内，带有score值(可选)的有序集成员的列表。
     */
    Set<String> zrangeByScore(String key, double min, double max, int offset, int count);

    // Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

    /**
     * 返回有序集key中，score值介于max和min之间(默认包括等于max或min)的所有的成员。<br>
     * 有序集成员按score值递减(从大到小)的次序排列。
     * <p>
     * 时间复杂度: O(log(N)+M)，N为有序集的基数，M为结果集的基数<br>
     * ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count] - http://redis.io/commands/zrevrangebyscore
     * 
     * @param key 键
     * @param max 检索的最大分数
     * @param min 检索的最小分数
     * @return 指定区间内，带有score值(可选)的有序集成员的列表。
     */
    Set<String> zrevrangeByScore(String key, double max, double min);

    /**
     * 返回有序集key中，score值介于max和min之间(默认包括等于max或min)的所有的成员。
     * <p>
     * 见{@link #zrevrangeByScore(String, double, double)}文档注释。
     * 
     * @param key 键
     * @param max 检索的最大分数
     * @param min 检索的最小分数
     * @param offset 返回列表的偏移量
     * @param count 返回列表的最大元素个数
     * @return 指定区间内，带有score值(可选)的有序集成员的列表。
     */
    Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count);

    // 元素计数
    /**
     * 返回有序集key的基数(元素数量)。
     * <p>
     * 时间复杂度: O(1)<br>
     * ZCARD key - http://redis.io/commands/zcard
     * 
     * @param key
     * @return 当key存在且是有序集类型时，返回有序集的基数；当key不存在时，返回0。
     */
    int zcard(String key);

    // 范围移除
    /**
     * 移除有序集key中，所有score值介于min和max之间(包括等于min或max)的成员。
     * <p>
     * 时间复杂度: O(log(N)+M)，N为有序集的基数，M为被移除成员的数量<br>
     * ZREMRANGEBYSCORE key min max - http://redis.io/commands/zremrangebyscore
     * 
     * @param key 键
     * @param min 检索的最小分数
     * @param max 检索的最大分数
     * @return 被移除成员的数量。
     */
    int zremrangeByScore(String key, double min, double max);

    /**
     * 移除有序集key中，指定排名(rank)区间内的所有成员。
     * <p>
     * 区间分别以下标参数start和stop指出，<font color="red">包含start和stop在内</font>。
     * <p>
     * 下标参数start和stop都以0为底，0处是分数最小的那个元素。<br>
     * 这些索引也可以是负数，表示位移从最高分处开始数。<br>
     * 例如，-1是分数最高的元素，-2是分数第二高的，依次类推。
     * <p>
     * 时间复杂度: O(log(N)+M)，N为有序集的基数，而M为被移除成员的数量<br>
     * ZREMRANGEBYRANK key start stop - http://redis.io/commands/zremrangebyrank
     * 
     * @param key
     * @param start
     * @param stop
     * @return 被移除成员的数量。
     */
    int zremrangeByRank(String key, int start, int stop);

    // =======================================================
    // Server (服务器) - http://redis.io/commands#server
    // =======================================================
    /**
     * 以一种易于解析且易于阅读的格式，返回关于Redis服务器的各种信息和统计数值。
     * <p>
     * INFO [section] - http://redis.io/commands/info
     * 
     * @param key
     * @param section
     * @return
     */
    String info(String key, String section);

}
