import random
from typing import Any

from pymongo.collection import Collection

BATCH_SIZE = 1000
HIT_RATE = 10
def get_names() -> list[str]:
    output = []
    with open('firstnames') as f:
        for line in f:
            output.append(f'{line.strip()}{random.randint(0, 9999)}')
    return output



def create(num: int, coll: Collection) -> list[str]:
    names = get_names()
    batch = []
    hits = []
    i=0
    while i < num:
        if len(batch) == BATCH_SIZE:
            coll.insert_many(batch)
            print(f'Inserted a batch; Total count: {i} out of {num}')
            batch = []
        doc = rand_doc(names)
        if i % HIT_RATE == 0:
            hits.append(doc['patientRecord']['ssn'])
        batch.append(doc)
        i += 1
    coll.insert_many(batch)
    print(f'Inserted a batch; Total count: {i} out of {num}')
    return hits


def rand_name(names: list[str]):
    idx = random.randint(0, len(names)-1)
    return names[idx]

def random_type():
    types = ['Visa', 'Check', 'Cash', 'MasterCard', 'Medicare', 'Medicaid']
    idx = random.randint(0, len(types)-1)
    return types[idx]

def rand_ssn():
    def _d():
        return random.randint(0,9)
    return f'{_d()}{_d()}{_d()}-{_d()}{_d()}-{_d()}{_d()}{_d()}{_d()}'

def get_misses(hits: list[str]) -> list[str]:
    output = []
    while len(output) < len(hits):
        cand = rand_ssn()
        if cand not in hits:
            output.append(cand)

    return output


def rand_doc(names: list[str]):
    return {
        "patientName": rand_name(names),
        "patientId": random.randint(0, 999999999),
        "patientRecord": {
            "ssn": rand_ssn(),
            "billing": {
                "type": random_type(),
                "number": str(random.randint(0, 99999999999))
            }
        }
    }