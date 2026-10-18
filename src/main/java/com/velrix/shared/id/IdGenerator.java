package com.velrix.shared.id;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import org.springframework.stereotype.Component;

/**
 * 与 M0 一致：主键由应用侧生成（MyBatis-Plus 雪花）。
 */
@Component
public class IdGenerator {

    private final DefaultIdentifierGenerator delegate = new DefaultIdentifierGenerator();

    public long nextId() {
        return delegate.nextId(null).longValue();
    }
}
