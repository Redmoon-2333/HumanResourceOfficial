"""
Redis 缓存性能测试分析脚本

分析 JMeter 测试结果，计算缓存优化效果

使用方法:
1. 运行 JMeter 测试（冷缓存和热缓存场景）
2. 运行此脚本：python eval/analyze_cache_performance.py

输出:
- 控制台输出：性能对比报告
- eval/jmeter_cache_test/performance_report.json
"""

import csv
import json
import os
import statistics
from datetime import datetime
from typing import List, Dict
from dataclasses import dataclass


@dataclass
class PerformanceStats:
    """性能统计"""
    label: str
    num_samples: int
    avg_response_time: float
    median_response_time: float
    p90_response_time: float
    p95_response_time: float
    p99_response_time: float
    min_response_time: float
    max_response_time: float
    std_dev: float
    error_rate: float


def parse_jmeter_csv(file_path: str) -> List[Dict]:
    """解析 JMeter CSV 结果文件"""
    results = []

    with open(file_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            try:
                elapsed = int(row.get('elapsed', 0))
                success = row.get('success', 'true') == 'true'
                results.append({
                    'elapsed': elapsed,
                    'success': success,
                    'label': row.get('label', '')
                })
            except (ValueError, KeyError) as e:
                continue

    return results


def compute_stats(results: List[Dict], label: str) -> PerformanceStats:
    """计算性能统计"""
    # 过滤出目标请求的结果
    elapsed_times = [r['elapsed'] for r in results if r['label'] == label]
    total_count = len(results)
    error_count = sum(1 for r in results if not r['success'] and r['label'] == label)

    if not elapsed_times:
        return None

    sorted_times = sorted(elapsed_times)
    n = len(sorted_times)

    return PerformanceStats(
        label=label,
        num_samples=n,
        avg_response_time=statistics.mean(elapsed_times),
        median_response_time=statistics.median(elapsed_times),
        p90_response_time=sorted_times[int(n * 0.90)],
        p95_response_time=sorted_times[int(n * 0.95)],
        p99_response_time=sorted_times[min(int(n * 0.99), n-1)],
        min_response_time=min(elapsed_times),
        max_response_time=max(elapsed_times),
        std_dev=statistics.stdev(elapsed_times) if n > 1 else 0,
        error_rate=error_count / n * 100 if n > 0 else 0
    )


def compare_performance(cold_stats: PerformanceStats, hot_stats: PerformanceStats) -> dict:
    """对比冷热缓存性能"""
    if not cold_stats or not hot_stats:
        return None

    avg_improvement = (cold_stats.avg_response_time - hot_stats.avg_response_time) / cold_stats.avg_response_time * 100
    p95_improvement = (cold_stats.p95_response_time - hot_stats.p95_response_time) / cold_stats.p95_response_time * 100

    return {
        'avg_improvement': avg_improvement,
        'p95_improvement': p95_improvement,
        'avg_speedup': cold_stats.avg_response_time / hot_stats.avg_response_time,
        'p95_speedup': cold_stats.p95_response_time / hot_stats.p95_response_time
    }


def print_report(cold_stats: PerformanceStats, hot_stats: PerformanceStats, comparison: dict):
    """打印性能对比报告"""
    print("\n" + "="*70)
    print("Redis 缓存性能测试报告")
    print("="*70)
    print(f"测试时间：{datetime.now().isoformat()}")
    print("-"*70)
    print()

    print("【冷缓存】（无缓存/清空 Redis）:")
    print(f"  样本数：{cold_stats.num_samples}")
    print(f"  平均响应时间：{cold_stats.avg_response_time:.0f}ms")
    print(f"  中位数响应时间：{cold_stats.median_response_time:.0f}ms")
    print(f"  P95 延迟：{cold_stats.p95_response_time:.0f}ms")
    print(f"  P99 延迟：{cold_stats.p99_response_time:.0f}ms")
    print(f"  错误率：{cold_stats.error_rate:.2f}%")
    print()

    print("【热缓存】（缓存命中）:")
    print(f"  样本数：{hot_stats.num_samples}")
    print(f"  平均响应时间：{hot_stats.avg_response_time:.0f}ms")
    print(f"  中位数响应时间：{hot_stats.median_response_time:.0f}ms")
    print(f"  P95 延迟：{hot_stats.p95_response_time:.0f}ms")
    print(f"  P99 延迟：{hot_stats.p99_response_time:.0f}ms")
    print(f"  错误率：{hot_stats.error_rate:.2f}%")
    print()

    print("-"*70)
    print("【性能提升】:")
    print(f"  平均响应时间提升：{comparison['avg_improvement']:.1f}%")
    print(f"  P95 延迟提升：{comparison['p95_improvement']:.1f}%")
    print(f"  平均加速比：{comparison['avg_speedup']:.2f}x")
    print(f"  P95 加速比：{comparison['p95_speedup']:.2f}x")
    print("="*70)

    # 判断是否达到 60% 提升
    if comparison['avg_improvement'] >= 60:
        print("✅ 接口响应时间降低 ≥ 60%，可以在简历中使用该声明")
    elif comparison['avg_improvement'] >= 50:
        print(f"⚠️  性能提升 {comparison['avg_improvement']:.1f}%，建议优化缓存策略后重测")
    else:
        print(f"⚠️  性能提升 {comparison['avg_improvement']:.1f}% < 60%")
        print("   建议：为高频查询接口添加@Cacheable，调整缓存 TTL")


def save_report(cold_stats: PerformanceStats, hot_stats: PerformanceStats,
                comparison: dict, output_path: str = "eval/jmeter_cache_test/performance_report.json"):
    """保存性能报告"""
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    report = {
        "timestamp": datetime.now().isoformat(),
        "cold_cache": {
            "num_samples": cold_stats.num_samples,
            "avg_response_time": round(cold_stats.avg_response_time, 2),
            "median_response_time": round(cold_stats.median_response_time, 2),
            "p95_response_time": round(cold_stats.p95_response_time, 2),
            "p99_response_time": round(cold_stats.p99_response_time, 2),
            "error_rate": round(cold_stats.error_rate, 2)
        },
        "hot_cache": {
            "num_samples": hot_stats.num_samples,
            "avg_response_time": round(hot_stats.avg_response_time, 2),
            "median_response_time": round(hot_stats.median_response_time, 2),
            "p95_response_time": round(hot_stats.p95_response_time, 2),
            "p99_response_time": round(hot_stats.p99_response_time, 2),
            "error_rate": round(hot_stats.error_rate, 2)
        },
        "improvement": {
            "avg_improvement_percent": round(comparison['avg_improvement'], 2),
            "p95_improvement_percent": round(comparison['p95_improvement'], 2),
            "avg_speedup": round(comparison['avg_speedup'], 2),
            "p95_speedup": round(comparison['p95_speedup'], 2)
        }
    }

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    print(f"\n性能报告已保存至：{output_path}")


def main():
    """主函数"""
    cold_csv = "eval/jmeter_cache_test/result_cold.csv"
    hot_csv = "eval/jmeter_cache_test/result_hot.csv"

    # 检查文件是否存在
    if not os.path.exists(cold_csv):
        print(f"❌ 冷缓存测试结果不存在：{cold_csv}")
        print("   请先运行 JMeter 测试（清空 Redis 场景）")
        print("   命令：jmeter -n -t eval/jmeter_cache_test/cache_performance_test.jmx -l eval/jmeter_cache_test/result_cold.csv")
        return

    if not os.path.exists(hot_csv):
        print(f"❌ 热缓存测试结果不存在：{hot_csv}")
        print("   请先运行 JMeter 测试（缓存命中场景）")
        print("   命令：jmeter -n -t eval/jmeter_cache_test/cache_performance_test.jmx -l eval/jmeter_cache_test/result_hot.csv")
        return

    # 解析结果
    print("解析 JMeter 测试结果...")
    cold_results = parse_jmeter_csv(cold_csv)
    hot_results = parse_jmeter_csv(hot_csv)

    # 计算统计
    print("计算性能统计...")
    cold_stats = compute_stats(cold_results, "GET /api/activity/list")
    hot_stats = compute_stats(hot_results, "GET /api/activity/list")

    if not cold_stats or not hot_stats:
        print("❌ 无法计算统计结果，请检查 CSV 文件格式")
        return

    # 对比性能
    comparison = compare_performance(cold_stats, hot_stats)

    # 打印报告
    print_report(cold_stats, hot_stats, comparison)

    # 保存报告
    save_report(cold_stats, hot_stats, comparison)


if __name__ == "__main__":
    main()
