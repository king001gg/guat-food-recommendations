package com.guatfood.config;

import com.guatfood.entity.Canteen;
import com.guatfood.entity.Dish;
import com.guatfood.entity.Favorite;
import com.guatfood.entity.Like;
import com.guatfood.entity.Rating;
import com.guatfood.entity.User;
import com.guatfood.entity.Window;
import com.guatfood.mapper.CanteenMapper;
import com.guatfood.mapper.DishMapper;
import com.guatfood.mapper.FavoriteMapper;
import com.guatfood.mapper.LikeMapper;
import com.guatfood.mapper.RatingMapper;
import com.guatfood.mapper.UserMapper;
import com.guatfood.mapper.WindowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 首次启动种子数据初始化：管理员/演示用户/食堂/档口/菜品/评分/点赞
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserMapper userMapper;
    @Autowired private CanteenMapper canteenMapper;
    @Autowired private WindowMapper windowMapper;
    @Autowired private DishMapper dishMapper;
    @Autowired private RatingMapper ratingMapper;
    @Autowired private LikeMapper likeMapper;
    @Autowired private FavoriteMapper favoriteMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        seed();
    }

    private void seed() {
        // ─── 用户 (密码均为 123456) ───
        User admin = user("admin", "管理员", "ADMIN");
        User u1 = user("zhangsan", "张三", "USER");
        User u2 = user("lisi", "李四", "USER");
        User u3 = user("wangwu", "王五", "USER");
        User u4 = user("zhaoliu", "赵六", "USER");
        User u5 = user("sunqi", "孙七", "USER");

        // ─── 食堂 ───
        Canteen c1 = canteen("天舟楼食堂", "南校区", 1);
        Canteen c2 = canteen("天宫楼食堂", "北校区", 2);
        Canteen c3 = canteen("莘子苑食堂", "东校区", 3);
        Canteen c4 = canteen("校外", "商业街", 4);

        // ─── 档口 ───
        Window w1 = window(c1.getId(), "桂林米粉", "桂林本地米粉，卤水香浓，锅烧脆香", "一楼");
        Window w2 = window(c1.getId(), "柳州螺蛳粉", "酸辣鲜香，配料十足，汤底浓郁", "一楼");
        Window w3 = window(c1.getId(), "自选快餐", "荤素自选，经济实惠，两荤一素管饱", "二楼");
        Window w4 = window(c2.getId(), "麻辣香锅", "现炒香锅，麻辣过瘾，可自选菜品", "一楼");
        Window w5 = window(c2.getId(), "黄焖鸡米饭", "鸡肉嫩滑，汤汁拌饭一绝", "一楼");
        Window w6 = window(c2.getId(), "兰州拉面", "手工拉面，汤清面劲，牛肉大块", "二楼");
        Window w7 = window(c3.getId(), "烤肉拌饭", "蜜汁烤肉，酱香浓郁，份量十足", "一楼");
        Window w8 = window(c3.getId(), "石锅拌饭", "韩式石锅，锅巴香脆，酱料地道", "二楼");
        Window w9 = window(c4.getId(), "港式烧腊", "叉烧烧鸭，皮脆肉嫩，港味正宗", "一楼");
        Window w10 = window(c4.getId(), "糖水铺", "广式糖水，清甜解腻，下午茶首选", "一楼");

        // ─── 菜品 ───
        Dish d1 = dish(w1.getId(), "桂林米粉", "招牌桂林米粉，锅烧+叉烧", "8.00");
        Dish d2 = dish(w1.getId(), "卤菜粉", "多卤菜版本，料足味浓", "9.00");
        Dish d3 = dish(w2.getId(), "招牌螺蛳粉", "酸笋+腐竹+花生，汤底浓郁", "12.00");
        Dish d4 = dish(w2.getId(), "干捞螺蛳粉", "无汤版本，酱香更浓", "13.00");
        Dish d5 = dish(w3.getId(), "两荤一素套餐", "自选两荤一素，米饭管饱", "12.00");
        Dish d6 = dish(w4.getId(), "麻辣香锅(自选)", "自选食材现炒，麻辣鲜香", "15.00");
        Dish d7 = dish(w5.getId(), "黄焖鸡米饭", "嫩滑鸡肉+浓香汤汁", "14.00");
        Dish d8 = dish(w6.getId(), "牛肉拉面", "手工拉面，大块牛肉", "12.00");
        Dish d9 = dish(w7.getId(), "蜜汁烤肉饭", "蜜汁烤肉，酱香四溢", "13.00");
        Dish d10 = dish(w8.getId(), "五花肉石锅拌饭", "锅巴香脆，韩式辣酱", "16.00");
        Dish d11 = dish(w9.getId(), "叉烧饭", "蜜汁叉烧，肥瘦相间", "18.00");
        Dish d12 = dish(w10.getId(), "杨枝甘露", "芒果+西柚，清甜解暑", "10.00");

        // ─── 档口评分 ───
        rate(u1, "WINDOW", w1.getId(), 5, 4, 4, "米粉很正宗，锅烧特别脆");
        rate(u2, "WINDOW", w1.getId(), 5, 5, 4, "性价比高，饭点排队也值");
        rate(u3, "WINDOW", w1.getId(), 4, 4, 4, "味道不错，就是汤有点咸");
        rate(u4, "WINDOW", w1.getId(), 5, 4, 5, "量足味道好");
        rate(u5, "WINDOW", w1.getId(), 4, 4, 3, "还不错");

        rate(u1, "WINDOW", w2.getId(), 5, 3, 4, "螺蛳粉天花板，酸笋很够味");
        rate(u2, "WINDOW", w2.getId(), 4, 3, 4, "好吃但略贵");
        rate(u3, "WINDOW", w2.getId(), 5, 4, 5, "配料很多，汤底浓郁");
        rate(u4, "WINDOW", w2.getId(), 4, 3, 4, "中规中矩");

        rate(u1, "WINDOW", w3.getId(), 3, 5, 4, "便宜管饱，口味一般");
        rate(u2, "WINDOW", w3.getId(), 3, 5, 5, "性价比之王");
        rate(u3, "WINDOW", w3.getId(), 3, 4, 4, "食堂打菜阿姨手不抖");

        rate(u1, "WINDOW", w4.getId(), 4, 3, 4, "香锅味道可以，就是贵了点");
        rate(u2, "WINDOW", w4.getId(), 5, 3, 4, "麻辣鲜香，每次都要排队");
        rate(u4, "WINDOW", w4.getId(), 4, 3, 3, "还行");

        rate(u1, "WINDOW", w5.getId(), 5, 4, 4, "黄焖鸡yyds，汤汁拌饭绝了");
        rate(u2, "WINDOW", w5.getId(), 5, 4, 5, "鸡肉嫩，分量足");
        rate(u3, "WINDOW", w5.getId(), 4, 4, 4, "好吃不贵");
        rate(u5, "WINDOW", w5.getId(), 5, 4, 4, "每周必吃");

        rate(u2, "WINDOW", w6.getId(), 4, 4, 4, "拉面筋道，牛肉也不少");
        rate(u3, "WINDOW", w6.getId(), 4, 4, 5, "汤很鲜");
        rate(u4, "WINDOW", w6.getId(), 3, 3, 3, "一般般，偏咸");

        rate(u1, "WINDOW", w7.getId(), 4, 4, 5, "烤肉饭量大，酱香浓郁");
        rate(u2, "WINDOW", w7.getId(), 5, 4, 5, "蜜汁烤肉一绝");
        rate(u5, "WINDOW", w7.getId(), 4, 4, 4, "好吃");

        rate(u3, "WINDOW", w8.getId(), 4, 3, 4, "锅巴很脆，酱料地道");
        rate(u4, "WINDOW", w8.getId(), 4, 3, 3, "还行，稍贵");
        rate(u1, "WINDOW", w8.getId(), 3, 3, 3, "一般");

        rate(u2, "WINDOW", w9.getId(), 4, 3, 3, "叉烧不错，就是贵");
        rate(u3, "WINDOW", w9.getId(), 5, 3, 4, "烧鸭皮很脆");
        rate(u5, "WINDOW", w9.getId(), 4, 3, 3, "味道可以");

        rate(u1, "WINDOW", w10.getId(), 4, 4, 3, "糖水清甜，适合下午");
        rate(u4, "WINDOW", w10.getId(), 5, 4, 4, "杨枝甘露很好喝");
        rate(u5, "WINDOW", w10.getId(), 4, 4, 3, "不错");

        // ─── 菜品评分 ───
        rate(u1, "DISH", d1.getId(), 5, 4, 4, "锅烧很脆，卤水香");
        rate(u2, "DISH", d1.getId(), 5, 5, 4, "最爱的桂林米粉");
        rate(u3, "DISH", d1.getId(), 4, 4, 4, "味道不错");

        rate(u2, "DISH", d2.getId(), 4, 4, 4, "卤菜很多");
        rate(u4, "DISH", d2.getId(), 4, 4, 4, "还可以");

        rate(u1, "DISH", d3.getId(), 5, 3, 4, "螺蛳粉一定要点这个");
        rate(u2, "DISH", d3.getId(), 5, 3, 4, "汤底很浓郁");
        rate(u5, "DISH", d3.getId(), 4, 3, 4, "不错");

        rate(u3, "DISH", d4.getId(), 4, 3, 4, "干捞更入味");
        rate(u4, "DISH", d4.getId(), 4, 3, 4, "可以");

        rate(u1, "DISH", d5.getId(), 3, 5, 4, "便宜实惠");
        rate(u2, "DISH", d5.getId(), 3, 5, 5, "管饱");

        rate(u1, "DISH", d6.getId(), 4, 3, 4, "香锅不错");
        rate(u2, "DISH", d6.getId(), 5, 3, 4, "麻辣过瘾");

        rate(u1, "DISH", d7.getId(), 5, 4, 4, "黄焖鸡yyds");
        rate(u2, "DISH", d7.getId(), 5, 4, 5, "汤汁拌饭绝了");
        rate(u3, "DISH", d7.getId(), 4, 4, 4, "好吃");

        rate(u2, "DISH", d8.getId(), 4, 4, 4, "牛肉大块");
        rate(u3, "DISH", d8.getId(), 4, 4, 5, "汤很鲜");

        rate(u1, "DISH", d9.getId(), 5, 4, 5, "蜜汁烤肉饭量大");
        rate(u2, "DISH", d9.getId(), 5, 4, 5, "酱香浓郁");

        rate(u3, "DISH", d10.getId(), 4, 3, 4, "锅巴脆");
        rate(u4, "DISH", d10.getId(), 4, 3, 3, "还行");

        rate(u2, "DISH", d11.getId(), 4, 3, 3, "叉烧不错");
        rate(u3, "DISH", d11.getId(), 5, 3, 4, "烧鸭皮脆");

        rate(u1, "DISH", d12.getId(), 4, 4, 3, "好喝");
        rate(u4, "DISH", d12.getId(), 5, 4, 4, "杨枝甘露yyds");
        rate(u5, "DISH", d12.getId(), 4, 4, 3, "清甜");

        // ─── 点赞 ───
        like(u1, "WINDOW", w1.getId());
        like(u2, "WINDOW", w1.getId());
        like(u3, "WINDOW", w1.getId());
        like(u1, "WINDOW", w2.getId());
        like(u2, "WINDOW", w2.getId());
        like(u1, "WINDOW", w5.getId());
        like(u2, "WINDOW", w5.getId());
        like(u3, "WINDOW", w5.getId());
        like(u1, "WINDOW", w7.getId());
        like(u2, "WINDOW", w7.getId());
        like(u1, "DISH", d1.getId());
        like(u2, "DISH", d1.getId());
        like(u1, "DISH", d3.getId());
        like(u2, "DISH", d3.getId());
        like(u1, "DISH", d7.getId());
        like(u2, "DISH", d7.getId());
        like(u1, "DISH", d9.getId());
        like(u2, "DISH", d9.getId());

        // ─── 收藏 ───
        favorite(u1, "WINDOW", w1.getId());
        favorite(u1, "WINDOW", w2.getId());
        favorite(u2, "WINDOW", w1.getId());
        favorite(u2, "DISH", d7.getId());
        favorite(u1, "DISH", d9.getId());
    }

    private User user(String username, String nickname, String role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode("123456"));
        u.setNickname(nickname);
        u.setAvatar(null);
        u.setRole(role);
        u.setStatus("ACTIVE");
        userMapper.insert(u);
        return u;
    }

    private Canteen canteen(String name, String location, int sort) {
        Canteen c = new Canteen();
        c.setName(name);
        c.setLocation(location);
        c.setSortOrder(sort);
        canteenMapper.insert(c);
        return c;
    }

    private Window window(Long canteenId, String name, String desc, String location) {
        Window w = new Window();
        w.setCanteenId(canteenId);
        w.setName(name);
        w.setDescription(desc);
        w.setCoverImage(null);
        w.setLocation(location);
        w.setStatus("PUBLISHED");
        w.setViewCount(0);
        windowMapper.insert(w);
        return w;
    }

    private Dish dish(Long windowId, String name, String desc, String price) {
        Dish d = new Dish();
        d.setWindowId(windowId);
        d.setName(name);
        d.setDescription(desc);
        d.setImage(null);
        d.setPrice(new BigDecimal(price));
        d.setStatus("PUBLISHED");
        d.setViewCount(0);
        dishMapper.insert(d);
        return d;
    }

    private void rate(User user, String type, Long targetId, int taste, int value, int portion, String comment) {
        Rating r = new Rating();
        r.setUserId(user.getId());
        r.setTargetType(type);
        r.setTargetId(targetId);
        r.setTaste(taste);
        r.setValueScore(value);
        r.setPortion(portion);
        r.setComment(comment);
        ratingMapper.insert(r);
    }

    private void like(User user, String type, Long targetId) {
        Like l = new Like();
        l.setUserId(user.getId());
        l.setTargetType(type);
        l.setTargetId(targetId);
        likeMapper.insert(l);
    }

    private void favorite(User user, String type, Long targetId) {
        Favorite f = new Favorite();
        f.setUserId(user.getId());
        f.setTargetType(type);
        f.setTargetId(targetId);
        favoriteMapper.insert(f);
    }
}
