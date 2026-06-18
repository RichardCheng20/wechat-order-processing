#!/usr/bin/env python3
"""从老板提供的 品名.xls 生成并重置商品目录 SQL。"""
from __future__ import annotations

import argparse
from collections import OrderedDict
from pathlib import Path

import xlrd

BLOCKS = [
    (0, '菇菌类'),
    (4, '花草香料类'),
    (8, '花草香料类'),
    (12, '蔬果类'),
    (16, '蔬果类'),
]
CAT_ORDER = ['菇菌类', '花草香料类', '蔬果类']


def esc(value: str) -> str:
    return value.replace('\\', '\\\\').replace("'", "''")


def parse_products(xls_path: Path) -> list[dict[str, str]]:
    book = xlrd.open_workbook(str(xls_path))
    sheet = book.sheet_by_name('Sheet2')
    products: list[dict[str, str]] = []
    seen: set[tuple[str, str]] = set()
    for row in range(2, sheet.nrows):
        for start, category in BLOCKS:
            name = str(sheet.cell_value(row, start + 1)).strip()
            unit = str(sheet.cell_value(row, start + 2)).strip() or '斤'
            if not name:
                continue
            key = (category, name)
            if key in seen:
                continue
            seen.add(key)
            products.append({'category': category, 'name': name, 'unit': unit})
    return products


def build_sql(products: list[dict[str, str]]) -> str:
    grouped: OrderedDict[str, list[dict[str, str]]] = OrderedDict(
        (cat, []) for cat in CAT_ORDER
    )
    for item in products:
        grouped[item['category']].append(item)

    lines = [
        '-- Reset catalog from 品名.xls (Sheet2)',
        'SET FOREIGN_KEY_CHECKS = 0;',
        'DELETE FROM order_items;',
        'DELETE FROM dispatch_logs;',
        'DELETE FROM payments;',
        'DELETE FROM purchase_payments;',
        'DELETE FROM orders;',
        'DELETE FROM product_prices;',
        'DELETE FROM products;',
        'DELETE FROM product_categories;',
        'SET FOREIGN_KEY_CHECKS = 1;',
        '',
        'INSERT INTO product_categories (merchant_id, parent_id, name, sort_order, status) VALUES',
    ]
    lines.append(
        ',\n'.join(
            f"(1, NULL, '{esc(cat)}', {idx}, 1)" for idx, cat in enumerate(CAT_ORDER, 1)
        ) + ';'
    )
    lines.append('')
    for cat in CAT_ORDER:
        for item in grouped[cat]:
            unit = esc(item['unit'])
            name = esc(item['name'])
            cat_name = esc(cat)
            lines.append(
                'INSERT INTO products (merchant_id, category_id, name, unit, sale_units, sale_status) '
                f"SELECT 1, id, '{name}', '{unit}', '{unit}', 'ON' FROM product_categories "
                f"WHERE merchant_id = 1 AND name = '{cat_name}' AND parent_id IS NULL LIMIT 1;"
            )
    return '\n'.join(lines) + '\n'


def main() -> None:
    parser = argparse.ArgumentParser(description='Generate product reset SQL from 品名.xls')
    parser.add_argument(
        'xls',
        nargs='?',
        default='/Users/frankfu/Documents/05_startup/品名.xls',
        help='Path to source xls file',
    )
    parser.add_argument(
        '-o',
        '--output',
        default=str(Path(__file__).resolve().parent / 'reset-products-from-catalog.sql'),
        help='Output SQL file path',
    )
    args = parser.parse_args()
    products = parse_products(Path(args.xls))
    sql = build_sql(products)
    out = Path(args.output)
    out.write_text(sql, encoding='utf-8')
    print(f'Generated {len(products)} products -> {out}')


if __name__ == '__main__':
    main()
