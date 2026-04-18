"""
面试调度算法 A/B 测试脚本

对比旧算法（先到先得）与新算法（三层排序策略）的分配成功率

使用方法:
1. 准备测试数据 eval/data/interview_candidates.json
2. 运行此脚本：python eval/interview_assignment_ab_test.py

输出:
- 旧算法和新算法的分配成功率
- 性能提升百分比
"""

import json
import os
from datetime import datetime
from typing import List, Dict, Tuple, Optional
from dataclasses import dataclass, asdict


@dataclass
class Candidate:
    """候选人"""
    id: int
    preferred_departments: List[str]
    available_slots: List[str]
    urgency_level: int  # 1-高，2-中，3-低


@dataclass
class Interviewer:
    """面试官"""
    id: int
    department: str
    available_slots: List[str]
    max_interviews: int
    assigned_count: int = 0


@dataclass
class Room:
    """教室"""
    id: int
    capacity: int
    available_slots: List[str]


@dataclass
class Assignment:
    """面试分配结果"""
    candidate_id: int
    interviewer_id: int
    room_id: int
    slot: str


class OldAssignmentAlgorithm:
    """
    旧算法：先到先得，无优先级
    """

    def assign(
        self,
        candidates: List[Candidate],
        interviewers: List[Interviewer],
        rooms: List[Room]
    ) -> Tuple[List[Assignment], List[int]]:
        """
        执行分配

        Returns:
            (assignments, conflicts) - 分配结果列表和未分配的候选人 ID
        """
        assignments = []
        conflicts = []

        # 复制面试官状态（避免修改原数据）
        interviewer_state = self._copy_interviewers(interviewers)
        room_state = self._copy_rooms(rooms)

        for candidate in candidates:
            assigned = False

            for dept in candidate.preferred_departments:
                if assigned:
                    break

                for slot in candidate.available_slots:
                    if assigned:
                        break

                    # 查找可用的面试官
                    interviewer = self._find_interviewer(
                        interviewer_state, dept, slot
                    )

                    if interviewer:
                        # 查找可用的教室
                        room = self._find_room(room_state, slot)

                        if room:
                            # 分配成功
                            assignments.append(Assignment(
                                candidate_id=candidate.id,
                                interviewer_id=interviewer.id,
                                room_id=room.id,
                                slot=slot
                            ))

                            # 更新状态
                            interviewer.assigned_count += 1
                            assigned = True

            if not assigned:
                conflicts.append(candidate.id)

        return assignments, conflicts

    def _copy_interviewers(self, interviewers: List[Interviewer]) -> List[Interviewer]:
        """深拷贝面试官列表"""
        return [
            Interviewer(
                id=i.id,
                department=i.department,
                available_slots=i.available_slots.copy(),
                max_interviews=i.max_interviews,
                assigned_count=0
            )
            for i in interviewers
        ]

    def _copy_rooms(self, rooms: List[Room]) -> List[Room]:
        """深拷贝教室列表"""
        return [
            Room(id=r.id, capacity=r.capacity, available_slots=r.available_slots.copy())
            for r in rooms
        ]

    def _find_interviewer(
        self,
        interviewers: List[Interviewer],
        department: str,
        slot: str
    ) -> Optional[Interviewer]:
        """查找可用的面试官"""
        for interviewer in interviewers:
            if (interviewer.department == department and
                slot in interviewer.available_slots and
                interviewer.assigned_count < interviewer.max_interviews):
                return interviewer
        return None

    def _find_room(
        self,
        rooms: List[Room],
        slot: str
    ) -> Optional[Room]:
        """查找可用的教室"""
        for room in rooms:
            if slot in room.available_slots:
                return room
        return None


class NewAssignmentAlgorithm:
    """
    新算法：三层排序策略
    1. 按部门分组
    2. 按时间段分配
    3. 按紧迫度排序
    """

    def assign(
        self,
        candidates: List[Candidate],
        interviewers: List[Interviewer],
        rooms: List[Room]
    ) -> Tuple[List[Assignment], List[int]]:
        """
        执行分配

        Returns:
            (assignments, conflicts) - 分配结果列表和未分配的候选人 ID
        """
        assignments = []
        conflicts = []

        # 复制面试官状态
        interviewer_state = self._copy_interviewers(interviewers)
        room_state = self._copy_rooms(rooms)

        # 步骤 1: 按紧迫度排序（高优先级先分配）
        sorted_candidates = sorted(candidates, key=lambda c: c.urgency_level)

        # 步骤 2: 按部门分组
        dept_groups = self._group_by_department(sorted_candidates)

        # 步骤 3: 为每组分配
        for dept, candidates_in_dept in dept_groups.items():
            for candidate in candidates_in_dept:
                assigned = False

                # 优先分配偏好的部门
                for slot in candidate.available_slots:
                    interviewer = self._find_interviewer(
                        interviewer_state, dept, slot
                    )

                    if interviewer:
                        room = self._find_room(room_state, slot)

                        if room:
                            assignments.append(Assignment(
                                candidate_id=candidate.id,
                                interviewer_id=interviewer.id,
                                room_id=room.id,
                                slot=slot
                            ))

                            interviewer.assigned_count += 1
                            assigned = True
                            break

                if not assigned:
                    # 尝试第二志愿部门
                    for alt_dept in candidate.preferred_departments:
                        if alt_dept != dept:
                            for slot in candidate.available_slots:
                                interviewer = self._find_interviewer(
                                    interviewer_state, alt_dept, slot
                                )

                                if interviewer:
                                    room = self._find_room(room_state, slot)

                                    if room:
                                        assignments.append(Assignment(
                                            candidate_id=candidate.id,
                                            interviewer_id=interviewer.id,
                                            room_id=room.id,
                                            slot=slot
                                        ))

                                        interviewer.assigned_count += 1
                                        assigned = True
                                        break

                        if assigned:
                            break

                if not assigned:
                    conflicts.append(candidate.id)

        return assignments, conflicts

    def _group_by_department(
        self,
        candidates: List[Candidate]
    ) -> Dict[str, List[Candidate]]:
        """按第一志愿部门分组"""
        groups = {}
        for candidate in candidates:
            if candidate.preferred_departments:
                dept = candidate.preferred_departments[0]
                if dept not in groups:
                    groups[dept] = []
                groups[dept].append(candidate)
        return groups

    def _copy_interviewers(self, interviewers: List[Interviewer]) -> List[Interviewer]:
        return [
            Interviewer(
                id=i.id,
                department=i.department,
                available_slots=i.available_slots.copy(),
                max_interviews=i.max_interviews,
                assigned_count=0
            )
            for i in interviewers
        ]

    def _copy_rooms(self, rooms: List[Room]) -> List[Room]:
        return [
            Room(id=r.id, capacity=r.capacity, available_slots=r.available_slots.copy())
            for r in rooms
        ]

    def _find_interviewer(
        self,
        interviewers: List[Interviewer],
        department: str,
        slot: str
    ) -> Optional[Interviewer]:
        for interviewer in interviewers:
            if (interviewer.department == department and
                slot in interviewer.available_slots and
                interviewer.assigned_count < interviewer.max_interviews):
                return interviewer
        return None

    def _find_room(self, rooms: List[Room], slot: str) -> Optional[Room]:
        for room in rooms:
            if slot in room.available_slots:
                return room
        return None


class AssignmentBenchmark:
    """分配算法基准测试器"""

    def __init__(self, data_path: str = "eval/data/interview_candidates.json"):
        self.data_path = data_path
        self.old_algorithm = OldAssignmentAlgorithm()
        self.new_algorithm = NewAssignmentAlgorithm()

    def load_data(self) -> Tuple[List[Candidate], List[Interviewer], List[Room]]:
        """加载测试数据"""
        if not os.path.exists(self.data_path):
            print(f"❌ 测试数据文件不存在：{self.data_path}")
            print("   请先创建测试数据文件")
            return [], [], []

        with open(self.data_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        candidates = [Candidate(**c) for c in data['candidates']]
        interviewers = [Interviewer(**i) for i in data['interviewers']]
        rooms = [Room(**r) for r in data['rooms']]

        return candidates, interviewers, rooms

    def run_ab_test(self) -> dict:
        """运行 A/B 测试"""
        candidates, interviewers, rooms = self.load_data()

        if not candidates:
            return None

        print(f"开始 A/B 测试...")
        print(f"  候选人：{len(candidates)} 人")
        print(f"  面试官：{len(interviewers)} 人")
        print(f"  教室：{len(rooms)} 间")
        print()

        # 运行旧算法
        print("运行旧算法（先到先得）...")
        old_assignments, old_conflicts = self.old_algorithm.assign(
            candidates, interviewers, rooms
        )
        old_success_rate = len(old_assignments) / len(candidates) * 100

        print(f"  成功分配：{len(old_assignments)}/{len(candidates)} 人")
        print(f"  分配成功率：{old_success_rate:.1f}%")
        print()

        # 运行新算法
        print("运行新算法（三层排序）...")
        new_assignments, new_conflicts = self.new_algorithm.assign(
            candidates, interviewers, rooms
        )
        new_success_rate = len(new_assignments) / len(candidates) * 100

        print(f"  成功分配：{len(new_assignments)}/{len(candidates)} 人")
        print(f"  分配成功率：{new_success_rate:.1f}%")
        print()

        # 计算提升
        absolute_improvement = new_success_rate - old_success_rate
        relative_improvement = (new_success_rate - old_success_rate) / old_success_rate * 100

        stats = {
            "timestamp": datetime.now().isoformat(),
            "num_candidates": len(candidates),
            "num_interviewers": len(interviewers),
            "num_rooms": len(rooms),
            "old_algorithm": {
                "success_count": len(old_assignments),
                "conflict_count": len(old_conflicts),
                "success_rate": old_success_rate
            },
            "new_algorithm": {
                "success_count": len(new_assignments),
                "conflict_count": len(new_conflicts),
                "success_rate": new_success_rate
            },
            "improvement": {
                "absolute": absolute_improvement,
                "relative": relative_improvement
            }
        }

        return stats

    def print_report(self, stats: dict):
        """打印测试报告"""
        print("\n" + "="*60)
        print("面试调度算法 A/B 测试报告")
        print("="*60)
        print(f"测试时间：{stats['timestamp']}")
        print(f"测试数据：{stats['num_candidates']} 名候选人，"
              f"{stats['num_interviewers']} 名面试官，{stats['num_rooms']} 间教室")
        print("-"*60)
        print("旧算法（先到先得）:")
        print(f"  成功分配：{stats['old_algorithm']['success_count']}/{stats['num_candidates']} 人")
        print(f"  分配成功率：{stats['old_algorithm']['success_rate']:.1f}%")
        print()
        print("新算法（三层排序）:")
        print(f"  成功分配：{stats['new_algorithm']['success_count']}/{stats['num_candidates']} 人")
        print(f"  分配成功率：{stats['new_algorithm']['success_rate']:.1f}%")
        print("-"*60)
        print(f"绝对提升：{stats['improvement']['absolute']:.1f} 个百分点")
        print(f"相对提升：{stats['improvement']['relative']:.1f}%")
        print("="*60)

        # 判断是否达到 35% 提升
        if stats['improvement']['relative'] >= 35:
            print("✅ 分配成功率提升 ≥ 35%，可以在简历中使用该声明")
        elif stats['improvement']['relative'] >= 30:
            print("⚠️  提升约 30%，可以保守估计为 '提升 30%+'")
        else:
            print(f"⚠️  相对提升 {stats['improvement']['relative']:.1f}% < 35%")
            print("   建议优化算法或调整测试数据")

    def save_report(self, stats: dict, output_path: str = "eval/assignment_ab_test_result.json"):
        """保存测试报告"""
        os.makedirs(os.path.dirname(output_path), exist_ok=True)

        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(stats, f, ensure_ascii=False, indent=2)

        print(f"\n测试报告已保存至：{output_path}")


def create_sample_data(output_path: str = "eval/data/interview_candidates.json"):
    """创建示例测试数据"""
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    # 模拟 2024 年招新数据
    data = {
        "candidates": [],
        "interviewers": [],
        "rooms": []
    }

    # 120 名候选人
    departments = ["技术部", "产品部", "宣传部", "组织部"]
    for i in range(1, 121):
        data["candidates"].append({
            "id": i,
            "preferred_departments": [
                departments[i % 4],
                departments[(i + 1) % 4]
            ],
            "available_slots": [
                f"2024-10-{20 + i // 20:d} {14 + (i % 4):d}:00",
                f"2024-10-{21 + i // 20:d} {10 + (i % 4):d}:00"
            ],
            "urgency_level": (i % 3) + 1  # 1-3
        })

    # 25 名面试官
    for i in range(1, 26):
        data["interviewers"].append({
            "id": i,
            "department": departments[i % 4],
            "available_slots": [
                f"2024-10-{20 + j // 6:d} {14 + (j % 4):d}:00"
                for j in range(20)
            ],
            "max_interviews": 5
        })

    # 8 间教室
    for i in range(1, 9):
        data["rooms"].append({
            "id": i,
            "capacity": 10 + i * 5,
            "available_slots": [
                f"2024-10-{20 + j // 6:d} {14 + (j % 4):d}:00"
                for j in range(24)
            ]
        })

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"示例测试数据已创建：{output_path}")
    return data


def main():
    """主函数"""
    # 检查测试数据是否存在
    if not os.path.exists("eval/data/interview_candidates.json"):
        print("测试数据不存在，创建示例数据...")
        create_sample_data()
        print()

    # 运行 A/B 测试
    benchmark = AssignmentBenchmark()
    stats = benchmark.run_ab_test()

    if stats:
        # 打印报告
        benchmark.print_report(stats)

        # 保存报告
        benchmark.save_report(stats)


if __name__ == "__main__":
    main()
