package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final ShopService shopService;
    private final PostService postService;
    private final PetShopItemRepository petShopItemRepository;

    public DataInitializer(UserService userService, ShopService shopService,
                           PostService postService, PetShopItemRepository petShopItemRepository) {
        this.userService = userService;
        this.shopService = shopService;
        this.postService = postService;
        this.petShopItemRepository = petShopItemRepository;
    }

    @Override
    public void run(String... args) {
        // 创建默认管理员
        if (!userService.existsByUsername("admin")) {
            User admin = new User("admin", "admin@food.com", "admin123", User.Role.ADMIN);
            admin.setName("系统管理员");
            userService.createAdminUser(admin);
            System.out.println("默认管理员账号已创建: admin / admin123");
        }

        // 创建测试用户
        if (!userService.existsByUsername("testuser")) {
            User user = new User("testuser", "test@example.com", "123456");
            user.setName("美食爱好者");
            user.setPhone("13800138000");
            user.setGender(1);
            user.setAge(25);
            user.setBio("热爱探索各种美食！");
            userService.registerUser(user);
            System.out.println("测试用户已创建: testuser / 123456");
        }

        // 创建示例商家（如果数据库中还没有已通过的商家）
        if (shopService.countApproved() == 0) {
            createSampleShops();
        }

        // 创建示例帖子
        createSamplePosts();

        // 初始化宠物商店道具
        initPetShopItems();
    }

    private void createSampleShops() {
        // 找到测试用户作为商家拥有者
        User owner = userService.findByUsername("testuser").orElse(null);
        if (owner == null) return;

        // 商家1：老王拉面
        Shop shop1 = new Shop();
        shop1.setShopName("老王拉面馆");
        shop1.setCategory("中餐");
        shop1.setCity("北京");
        shop1.setDistrict("海淀区");
        shop1.setLocation("北京市海淀区中关村大街88号");
        shop1.setPhone("010-88886666");
        shop1.setStartTime("08:00");
        shop1.setEndTime("22:00");
        shop1.setIntroduction("传承三代的老字号拉面馆，手工拉面，汤底浓郁，深受食客喜爱。招牌红烧牛肉面连续五年被评为必吃榜推荐。");
        shop1.setStatus(Shop.ShopStatus.APPROVED);
        Shop saved1 = shopService.applyShop(shop1, owner);
        shopService.approveShop(saved1.getId());

        Dish d1 = new Dish();
        d1.setDishName("红烧牛肉面");
        d1.setPrice(new BigDecimal("28.00"));
        d1.setDescription("精选上等牛腱肉，配以秘制香料慢炖4小时，汤汁浓郁鲜美");
        shopService.addDish(d1, saved1.getId());

        Dish d2 = new Dish();
        d2.setDishName("酸辣面");
        d2.setPrice(new BigDecimal("22.00"));
        d2.setDescription("手工拉面配上特制酸辣汤底，开胃爽口");
        shopService.addDish(d2, saved1.getId());

        Dish d3 = new Dish();
        d3.setDishName("炸酱面");
        d3.setPrice(new BigDecimal("20.00"));
        d3.setDescription("传统老北京风味，黄瓜丝配肉酱，地道京味");
        shopService.addDish(d3, saved1.getId());

        // 商家2：樱花寿司
        Shop shop2 = new Shop();
        shop2.setShopName("樱花寿司");
        shop2.setCategory("日料");
        shop2.setLocation("北京市朝阳区三里屯太古里B1层");
        shop2.setPhone("010-66668888");
        shop2.setStartTime("11:00");
        shop2.setEndTime("23:00");
        shop2.setIntroduction("正宗日式料理，食材新鲜空运，主厨拥有20年日料经验。环境优雅，适合约会和商务宴请。");
        shop2.setStatus(Shop.ShopStatus.APPROVED);
        Shop saved2 = shopService.applyShop(shop2, owner);
        shopService.approveShop(saved2.getId());

        Dish d4 = new Dish();
        d4.setDishName("三文鱼刺身");
        d4.setPrice(new BigDecimal("68.00"));
        d4.setDescription("挪威进口三文鱼，肥美鲜嫩，入口即化");
        shopService.addDish(d4, saved2.getId());

        Dish d5 = new Dish();
        d5.setDishName("鳗鱼饭");
        d5.setPrice(new BigDecimal("58.00"));
        d5.setDescription("炭烤鳗鱼配特制酱汁，米饭粒粒分明");
        shopService.addDish(d5, saved2.getId());

        Dish d6 = new Dish();
        d6.setDishName("抹茶提拉米苏");
        d6.setPrice(new BigDecimal("38.00"));
        d6.setDescription("日本进口宇治抹茶，层层细腻，甜而不腻");
        shopService.addDish(d6, saved2.getId());

        // 商家3：披萨工坊
        Shop shop3 = new Shop();
        shop3.setShopName("披萨工坊");
        shop3.setCategory("西餐");
        shop3.setCity("上海");
        shop3.setDistrict("静安区");
        shop3.setLocation("上海市静安区南京西路1266号");
        shop3.setPhone("021-55556666");
        shop3.setStartTime("10:00");
        shop3.setEndTime("23:30");
        shop3.setIntroduction("意大利传统手工披萨，采用进口芝士和新鲜配料，窑炉现烤，外脆内软。");
        shop3.setStatus(Shop.ShopStatus.APPROVED);
        Shop saved3 = shopService.applyShop(shop3, owner);
        shopService.approveShop(saved3.getId());

        Dish d7 = new Dish();
        d7.setDishName("玛格丽特披萨");
        d7.setPrice(new BigDecimal("78.00"));
        d7.setDescription("经典意式披萨，新鲜马苏里拉芝士配罗勒叶");
        shopService.addDish(d7, saved3.getId());

        Dish d8 = new Dish();
        d8.setDishName("意式肉酱面");
        d8.setPrice(new BigDecimal("45.00"));
        d8.setDescription("手工意面配慢炖牛肉酱，撒上帕尔马干酪");
        shopService.addDish(d8, saved3.getId());

        System.out.println("示例商家和菜品数据已创建");
    }

    private void createSamplePosts() {
        Optional<User> testUser = userService.findByUsername("testuser");
        if (testUser.isEmpty()) return;
        Long userId = testUser.get().getId();

        // 只在没有任何帖子时创建
        if (postService.countByUser(userId) > 0) return;

        postService.createPost(userId,
                "北京必吃的三家宝藏小店",
                "作为一个在北京生活了五年的吃货，今天给大家分享三家我反复光顾的宝藏小店！\n\n第一家是海淀的「老王拉面馆」，他们家的红烧牛肉面简直绝了，牛腱肉炖得软烂入味，汤底浓郁但不油腻，每次去都必点！\n\n第二家是朝阳三里屯的「樱花寿司」，三文鱼刺身肥美鲜嫩，入口即化，环境也很适合拍照打卡。\n\n第三家虽然在上海，但如果有机会去一定要试试「披萨工坊」，玛格丽特披萨是我的最爱！",
                null, "北京");

        postService.createPost(userId,
                "周末探店 | 三里屯美食地图",
                "周末和朋友一起逛三里屯，整理了一份美食地图分享给大家~\n\n这次一共吃了三家店，从日料到火锅再到甜品，每一顿都没有踩雷。特别推荐樱花寿司的鳗鱼饭，炭烤的鳗鱼配上特制酱汁，真的太香了！\n\n下次打算把国贸附近的店也整理一下，大家有什么推荐吗？",
                null, "北京朝阳区三里屯");

        postService.createPost(userId,
                "一个人也要好好吃饭 | 快手晚餐分享",
                "今天下班后去老王拉面馆吃了一碗炸酱面，作为北方人真的是从小吃到大的味道。\n\n手擀的面条劲道有嚼劲，配上浓郁的肉酱和清爽的黄瓜丝，简简单单却很满足。\n\n一个人吃饭也要认真对待每一餐呀！你们下班后一般都吃什么呢？",
                null, "北京市海淀区");
    }

    // ==================== 宠物商店道具初始化 ====================

    private void initPetShopItems() {
        if (petShopItemRepository.count() > 0) return;

        // 食物类：增加经验
        addShopItem("宠物饼干", 10, PetShopItem.ItemType.FOOD, "exp", 30,
                "普通宠物饼干，+30经验", 1);
        addShopItem("美味鱼干", 25, PetShopItem.ItemType.FOOD, "exp", 80,
                "风干小鱼，+80经验", 2);
        addShopItem("豪华套餐", 60, PetShopItem.ItemType.FOOD, "exp", 200,
                "精心搭配的营养大餐，+200经验", 3);
        addShopItem("神秘仙果", 150, PetShopItem.ItemType.FOOD, "exp", 500,
                "传说中吃了能飞速成长的果实，+500经验", 4);

        // 装扮类：改变外观
        addShopItem("小黄帽", 50, PetShopItem.ItemType.DECORATION, "hat",
                "一顶可爱的黄色小帽子", 10);
        addShopItem("魔法斗篷", 80, PetShopItem.ItemType.DECORATION, "outfit",
                "闪亮的魔法斗篷，穿上超酷", 11);
        addShopItem("蝴蝶结", 40, PetShopItem.ItemType.DECORATION, "accessory",
                "粉色的蝴蝶结，可爱加倍", 12);
        addShopItem("皇冠", 200, PetShopItem.ItemType.DECORATION, "hat",
                "金光闪闪的皇冠，宠物之王", 13);
        addShopItem("圣诞毛衣", 100, PetShopItem.ItemType.DECORATION, "outfit",
                "温暖的圣诞主题毛衣", 14);

        // 特殊道具
        addShopItem("彩虹特效", 300, PetShopItem.ItemType.SPECIAL, "effect",
                "让宠物全身散发彩虹光芒", 20);

        System.out.println("宠物商店默认道具已创建 (共10件)");
    }

    private void addShopItem(String name, int price, PetShopItem.ItemType type,
                             String effectKey, String description, int sortOrder) {
        PetShopItem item = new PetShopItem();
        item.setName(name);
        item.setPrice(price);
        item.setType(type);
        item.setEffectKey(effectKey);
        item.setEffectValue(type == PetShopItem.ItemType.FOOD ? Integer.parseInt(effectKey) : 0);
        item.setDescription(description);
        item.setSortOrder(sortOrder);
        item.setActive(true);
        petShopItemRepository.save(item);
    }

    private void addShopItem(String name, int price, PetShopItem.ItemType type,
                             String effectKey, int effectValue, String description, int sortOrder) {
        PetShopItem item = new PetShopItem();
        item.setName(name);
        item.setPrice(price);
        item.setType(type);
        item.setEffectKey(effectKey);
        item.setEffectValue(effectValue);
        item.setDescription(description);
        item.setSortOrder(sortOrder);
        item.setActive(true);
        petShopItemRepository.save(item);
    }
}
