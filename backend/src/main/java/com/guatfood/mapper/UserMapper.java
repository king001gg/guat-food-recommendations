package com.guatfood.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guatfood.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 服务端注销：递增版本号，使已签发 Token 全部失效 */
    @Update("UPDATE `user` SET token_version = token_version + 1 WHERE id = #{id}")
    int incrementTokenVersion(@Param("id") Long id);

    /** 修改/重置密码并同时吊销旧 Token */
    @Update("UPDATE `user` SET password = #{password}, token_version = token_version + 1 WHERE id = #{id}")
    int updatePasswordAndIncrementVersion(@Param("id") Long id, @Param("password") String password);
}
