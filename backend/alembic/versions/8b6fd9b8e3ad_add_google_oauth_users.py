"""add google oauth users

Revision ID: 8b6fd9b8e3ad
Revises: dff46c684cc4
Create Date: 2026-05-06 12:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "8b6fd9b8e3ad"
down_revision: Union[str, Sequence[str], None] = "dff46c684cc4"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.alter_column("users", "password_hash", existing_type=sa.String(length=255), nullable=True)
    op.add_column("users", sa.Column("oauth_provider", sa.String(length=50), nullable=True))
    op.add_column("users", sa.Column("oauth_subject", sa.String(length=255), nullable=True))
    op.create_unique_constraint(
        "uq_users_oauth_provider_subject",
        "users",
        ["oauth_provider", "oauth_subject"],
    )


def downgrade() -> None:
    op.drop_constraint("uq_users_oauth_provider_subject", "users", type_="unique")
    op.drop_column("users", "oauth_subject")
    op.drop_column("users", "oauth_provider")
    op.alter_column("users", "password_hash", existing_type=sa.String(length=255), nullable=False)
