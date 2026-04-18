"""
缓存性能测试辅助脚本

用于自动化执行 JMeter 测试（冷缓存和热缓存场景）

使用方法:
python eval/run_jmeter_cache_test.py
"""

import os
import subprocess
import time
from datetime import datetime


class JMeterCacheTest:
    """JMeter 缓存测试执行器"""

    def __init__(self):
        self.base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.jmeter_path = self._find_jmeter()

    def _find_jmeter(self) -> str:
        """查找 JMeter 安装路径"""
        possible_paths = [
            r"d:\tools\apache-jmeter-5.6\bin\jmeter.bat",
            r"d:\tools\apache-jmeter-5.5\bin\jmeter.bat",
            r"d:\tools\jmeter\bin\jmeter.bat",
            r"c:\tools\apache-jmeter-5.6\bin\jmeter.bat",
        ]

        for path in possible_paths:
            if os.path.exists(path):
                return path

        # 尝试在 PATH 中查找
        try:
            result = subprocess.run(
                ["jmeter", "--version"],
                capture_output=True,
                timeout=5
            )
            if result.returncode == 0:
                return "jmeter"
        except (subprocess.TimeoutExpired, FileNotFoundError):
            pass

        return None

    def flush_redis(self):
        """清空 Redis"""
        print("正在清空 Redis...")
        try:
            result = subprocess.run(
                ["redis-cli", "FLUSHDB"],
                capture_output=True,
                text=True,
                timeout=5
            )
            if "OK" in result.stdout:
                print("✅ Redis 已清空")
                return True
            else:
                print(f"❌ Redis 清空失败：{result.stderr}")
                return False
        except FileNotFoundError:
            print("⚠️  未找到 redis-cli，请手动清空 Redis")
            print("   命令：redis-cli FLUSHDB")
            return False
        except Exception as e:
            print(f"❌ 错误：{e}")
            return False

    def warmup_cache(self):
        """预热缓存"""
        print("正在预热缓存...")
        try:
            response = subprocess.run(
                ["curl", "-s", "http://localhost:8080/api/activity/list"],
                capture_output=True,
                timeout=10
            )
            if response.returncode == 0:
                print("✅ 缓存已预热")
                return True
            else:
                print("⚠️  缓存预热失败，请确保 Spring Boot 应用正在运行")
                return False
        except FileNotFoundError:
            # Windows 可能没有 curl
            print("⚠️  未找到 curl 命令，请手动访问 http://localhost:8080/api/activity/list 预热缓存")
            return False
        except Exception as e:
            print(f"❌ 错误：{e}")
            return False

    def run_jmeter_test(self, output_csv: str):
        """运行 JMeter 测试"""
        jmx_file = os.path.join(self.base_dir, "eval", "jmeter_cache_test", "cache_performance_test.jmx")

        if not os.path.exists(jmx_file):
            print(f"❌ JMX 文件不存在：{jmx_file}")
            return False

        if self.jmeter_path is None:
            print("❌ 未找到 JMeter，请先安装")
            print("   下载地址：https://jmeter.apache.org/download_jmeter.cgi")
            return False

        print(f"正在运行 JMeter 测试...")
        print(f"JMX 文件：{jmx_file}")
        print(f"输出文件：{output_csv}")

        try:
            cmd = [
                self.jmeter_path if self.jmeter_path != "jmeter" else "jmeter",
                "-n",  # 非 GUI 模式
                "-t", jmx_file,
                "-l", output_csv
            ]

            result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)

            if result.returncode == 0:
                print(f"✅ JMeter 测试完成")
                return True
            else:
                print(f"❌ JMeter 测试失败：{result.stderr}")
                return False

        except subprocess.TimeoutExpired:
            print("❌ JMeter 测试超时（>5 分钟）")
            return False
        except Exception as e:
            print(f"❌ 错误：{e}")
            return False

    def run_full_test(self):
        """运行完整的缓存测试流程"""
        print("\n" + "="*70)
        print("Redis 缓存性能测试")
        print("="*70)
        print(f"开始时间：{datetime.now().isoformat()}")
        print()

        # 检查 JMeter
        if self.jmeter_path is None:
            print("❌ 未找到 JMeter，测试终止")
            return False

        print(f"JMeter 路径：{self.jmeter_path}")

        # 场景 1：冷缓存测试
        print("\n" + "-"*70)
        print("【场景 1】冷缓存测试（清空 Redis）")
        print("-"*70)

        if not self.flush_redis():
            print("⚠️  Redis 清空失败，但将继续测试...")
            time.sleep(2)

        cold_csv = os.path.join(self.base_dir, "eval", "jmeter_cache_test", "result_cold.csv")
        if not self.run_jmeter_test(cold_csv):
            print("❌ 冷缓存测试失败")
            return False
        print(f"✅ 冷缓存测试完成，结果保存至：{cold_csv}")

        # 场景 2：热缓存测试
        print("\n" + "-"*70)
        print("【场景 2】热缓存测试（缓存命中）")
        print("-"*70)

        if not self.warmup_cache():
            print("⚠️  缓存预热失败，但将继续测试...")

        # 等待缓存生效
        print("等待 5 秒让缓存生效...")
        time.sleep(5)

        hot_csv = os.path.join(self.base_dir, "eval", "jmeter_cache_test", "result_hot.csv")
        if not self.run_jmeter_test(hot_csv):
            print("❌ 热缓存测试失败")
            return False
        print(f"✅ 热缓存测试完成，结果保存至：{hot_csv}")

        # 分析结果
        print("\n" + "-"*70)
        print("分析测试结果...")
        print("-"*70)

        self.analyze_results(cold_csv, hot_csv)

        print("\n" + "="*70)
        print(f"测试完成时间：{datetime.now().isoformat()}")
        print("="*70)

        return True

    def analyze_results(self, cold_csv: str, hot_csv: str):
        """调用分析脚本"""
        analyze_script = os.path.join(self.base_dir, "eval", "analyze_cache_performance.py")

        if os.path.exists(analyze_script):
            subprocess.run(["python", analyze_script])
        else:
            print(f"⚠️  分析脚本不存在：{analyze_script}")


def main():
    """主函数"""
    test = JMeterCacheTest()
    success = test.run_full_test()

    if success:
        print("\n✅ 所有测试完成!")
    else:
        print("\n❌ 测试未完成，请检查错误信息")


if __name__ == "__main__":
    main()
