"""
SSE 流式输出性能测试脚本

测量首字延迟（Time to First Token, TTFT）

使用方法:
1. 确保 Spring Boot 应用正在运行 (默认端口 8080)
2. 运行此脚本：python eval/benchmark_sse.py

输出:
- 平均首字延迟
- P50/P95/P99 延迟
- 性能测试报告
"""

import aiohttp
import asyncio
import statistics
import time
import json
import os
from datetime import datetime
from typing import List, Tuple


class SSEBenchmark:
    """SSE 性能基准测试器"""

    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.results: List[float] = []

    async def measure_first_token(
        self,
        session: aiohttp.ClientSession,
        prompt: str,
        timeout: int = 30
    ) -> float:
        """
        测量单个请求的首字延迟

        Args:
            session: aiohttp 会话
            prompt: 测试提示词
            timeout: 超时时间（秒）

        Returns:
            首字延迟（毫秒），失败返回 -1
        """
        start = time.time()

        try:
            async with session.post(
                f'{self.base_url}/api/ai/chat-stream',
                json={"message": prompt},
                headers={"Content-Type": "application/json"},
                timeout=aiohttp.ClientTimeout(total=timeout)
            ) as response:
                if response.status != 200:
                    print(f"  HTTP {response.status} 错误")
                    return -1

                async for line in response.content:
                    line = line.strip()
                    if line:
                        # 收到第一个非空 SSE 消息
                        elapsed_ms = (time.time() - start) * 1000
                        return elapsed_ms

        except asyncio.TimeoutError:
            print(f"  超时 ({timeout}s)")
            return -1
        except aiohttp.ClientError as e:
            print(f"  网络错误：{e}")
            return -1
        except Exception as e:
            print(f"  未知错误：{e}")
            return -1

        return -1

    async def run_benchmark(
        self,
        prompt: str = "你好，请问如何申请活动场地？",
        num_requests: int = 100,
        concurrent_users: int = 10,
        warmup_requests: int = 5
    ) -> dict:
        """
        运行基准测试

        Args:
            prompt: 测试提示词
            num_requests: 总请求数
            concurrent_users: 并发用户数
            warmup_requests: 预热请求数

        Returns:
            性能统计字典
        """
        print(f"开始 SSE 性能测试...")
        print(f"  提示词：{prompt}")
        print(f"  总请求数：{num_requests}")
        print(f"  并发用户数：{concurrent_users}")
        print()

        # 预热
        print("预热阶段...")
        async with aiohttp.ClientSession() as session:
            for i in range(warmup_requests):
                await self.measure_first_token(session, prompt)
        print("预热完成\n")

        # 正式测试
        semaphore = asyncio.Semaphore(concurrent_users)
        self.results = []

        async def limited_request(session, prompt):
            async with semaphore:
                result = await self.measure_first_token(session, prompt)
                if result > 0:
                    self.results.append(result)
                return result

        async with aiohttp.ClientSession() as session:
            tasks = [limited_request(session, prompt) for _ in range(num_requests)]
            await asyncio.gather(*tasks)

        # 过滤失败的结果
        valid_results = [r for r in self.results if r > 0]

        if not valid_results:
            print("❌ 所有请求都失败了，请检查服务是否正常运行")
            return None

        return self._compute_statistics(valid_results)

    def _compute_statistics(self, results: List[float]) -> dict:
        """计算统计数据"""
        sorted_results = sorted(results)
        n = len(sorted_results)

        stats = {
            "timestamp": datetime.now().isoformat(),
            "num_samples": n,
            "mean": statistics.mean(results),
            "median": statistics.median(results),
            "std_dev": statistics.stdev(results) if n > 1 else 0,
            "min": min(results),
            "max": max(results),
            "p50": sorted_results[int(n * 0.50)],
            "p90": sorted_results[int(n * 0.90)],
            "p95": sorted_results[int(n * 0.95)],
            "p99": sorted_results[min(int(n * 0.99), n-1)],
        }

        return stats

    def print_report(self, stats: dict):
        """打印性能报告"""
        print("\n" + "="*60)
        print("SSE 流式输出性能测试报告")
        print("="*60)
        print(f"测试时间：{stats['timestamp']}")
        print(f"有效样本数：{stats['num_samples']}")
        print("-"*60)
        print(f"平均首字延迟：{stats['mean']:.0f}ms")
        print(f"中位数 (P50):  {stats['median']:.0f}ms")
        print(f"P90 延迟：      {stats['p90']:.0f}ms")
        print(f"P95 延迟：      {stats['p95']:.0f}ms")
        print(f"P99 延迟：      {stats['p99']:.0f}ms")
        print(f"最小值：        {stats['min']:.0f}ms")
        print(f"最大值：        {stats['max']:.0f}ms")
        print(f"标准差：        {stats['std_dev']:.0f}ms")
        print("="*60)

        # 判断是否达到 300ms 目标
        if stats['mean'] <= 300:
            print("✅ 首字延迟 ≤ 300ms，可以在简历中使用该声明")
        else:
            print(f"⚠️  平均首字延迟 {stats['mean']:.0f}ms > 300ms")
            print("   建议优化：启用 Redis 缓存、使用 Reactor 模式、优化向量检索")

    def save_report(self, stats: dict, output_path: str = "eval/sse_benchmark_result.json"):
        """保存性能报告"""
        os.makedirs(os.path.dirname(output_path), exist_ok=True)

        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(stats, f, ensure_ascii=False, indent=2)

        print(f"\n性能报告已保存至：{output_path}")


async def main():
    """主函数"""
    # 初始化基准测试器
    benchmark = SSEBenchmark("http://localhost:8080")

    # 运行测试
    stats = await benchmark.run_benchmark(
        prompt="你好，请问如何申请活动场地？",
        num_requests=100,
        concurrent_users=10
    )

    if stats:
        # 打印报告
        benchmark.print_report(stats)

        # 保存报告
        benchmark.save_report(stats)


if __name__ == "__main__":
    asyncio.run(main())
