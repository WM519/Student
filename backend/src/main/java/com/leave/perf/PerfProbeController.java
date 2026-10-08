package com.leave.perf;

import com.leave.common.Result;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 性能测试专用探针（仅 perf profile 生效，默认运行环境不加载）。
 *
 * <p>用途：在受控测试环境下用 {@code delayMs} 稳定占用 Tomcat 工作线程，
 * 从而在可观测的并发区间内复现“响应时间拐点 / 吞吐量拐点”以及“线程池被占满后的系统雪崩”。
 * 该接口不属于业务功能，仅在被测系统的性能测试环境启用，不会影响默认 profile 的行为。</p>
 */
@RestController
@RequestMapping("/api/perf")
@Profile("perf")
public class PerfProbeController {

    /** 当前正在占用（睡眠中）的探针线程数，用于观察线程池是否被打满 */
    private final AtomicInteger currentBusy = new AtomicInteger();
    /** 历史最大并发占用量 */
    private final AtomicInteger maxBusy = new AtomicInteger();

    /**
     * 可控延迟探针。
     *
     * @param delayMs 模拟业务处理耗时（毫秒），范围 0~5000
     */
    @GetMapping("/probe")
    public Result<Map<String, Object>> probe(
            @RequestParam(name = "delayMs", defaultValue = "200") long delayMs) throws InterruptedException {
        long delay = Math.max(0, Math.min(delayMs, 5000));
        int busy = currentBusy.incrementAndGet();
        maxBusy.accumulateAndGet(busy, Math::max);
        try {
            Thread.sleep(delay);
        } finally {
            currentBusy.decrementAndGet();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("delayMs", delay);
        data.put("busyThreads", busy);
        data.put("serverTime", System.currentTimeMillis());
        return Result.ok(data);
    }

    /** 轻量探针：用于与延迟探针做对照，验证“正常接口是否被拖慢” */
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pong", true);
        data.put("serverTime", System.currentTimeMillis());
        return Result.ok(data);
    }

    /** 运行状态：并发占用与 JVM 线程数，用于雪崩现象取证 */
    @GetMapping("/threads")
    public Result<Map<String, Object>> threads() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("busyThreads", currentBusy.get());
        data.put("maxBusyThreads", maxBusy.get());
        data.put("jvmLiveThreads", ManagementFactory.getThreadMXBean().getThreadCount());
        data.put("heapUsedMB", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
        return Result.ok(data);
    }
}
